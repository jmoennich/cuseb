/*  
 * Thanks to Romain Guy and Kevin Long who wrote/modified the original code
 * that can be found here:
 * http://www.curious-creature.org/2005/07/09/a-music-shelf-in-java2d/
 * 
 * Copyright 2012 Jan Mönnich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cuseb.flow;

import de.cuseb.data.Book;
import de.cuseb.data.BookSelectionListener;
import de.cuseb.table.BookTableModel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class BookFlowPanel extends JPanel {

    private static final double ANIM_SCROLL_DELAY = 300;
    private static final int CD_SIZE = 250;
    private int displayWidth = CD_SIZE;
    private int displayHeight = (int) (CD_SIZE * 2 / 1.12);
    private List<BookFlowItem> avatars = null;
    private String avatarText = "   ";
    private Timer scrollerTimer = null;
    private Timer faderTimer = null;
    private float alphaLevel = 0.0f;
    private int avatarAmount = 3;
    private int avatarIndex = -1;
    private double avatarPosition = 0.0;
    private double avatarSpacing = 0.45;
    private double sigma;
    private double rho;
    private double exp_multiplier;
    private double exp_member;
    private boolean damaged = true;
    private DrawableAvatar[] drawableAvatars;
    private KeyScroller keyScroller;
    private FocusGrabber focusGrabber;
    private CursorChanger cursorChanger;
    private AvatarScroller avatarScroller;
    private MouseWheelScroller wheelScroller;
    private KeyAvatarSelector keyAvatarSelector;
    private MouseAvatarSelector mouseAvatarSelector;
    private List<ListSelectionListener> listSelectionListeners;
    private List<BookSelectionListener> panelListeners;
    private Font titleFont;

    public BookFlowPanel() {
        titleFont = getFont().deriveFont(Font.PLAIN);
        listSelectionListeners = new ArrayList<ListSelectionListener>();
        panelListeners = new ArrayList<BookSelectionListener>();
        avatars = new ArrayList<BookFlowItem>();
        this.sigma = 0.60;
        this.rho = 1.0;
        computeEquationParts();
        this.rho = computeModifierUnprotected(0.0);
        computeEquationParts();
        setBackground(Color.BLACK);
        addComponentListener(new DamageManager());
        initInputListeners();
        addInputListeners();
    }

    public void update(JTable table) {
        avatars.clear();
        BookTableModel model = (BookTableModel) table.getModel();
        for (int i = 0; i < table.getRowCount(); i++) {
            Book book = model.getBookAtRow(
                    table.getRowSorter().convertRowIndexToModel(i));
            avatars.add(new BookFlowItem(book));
        }
        avatarAmount = avatars.size();
        setAvatarIndex(0);
        setPosition(0);
        startFader();
    }

    public Book getSelectedBook() {
        return avatars.get(avatarIndex).getBook();
    }

    private void setPosition(double position) {
        this.avatarPosition = position;
        this.damaged = true;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(displayWidth * 3, (int) (displayHeight));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isShowing()) {
            return;
        }
        super.paintComponent(g);
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        Composite oldComposite = g2.getComposite();
        if (damaged) {
            drawableAvatars = sortAvatarsByDepth(x, y, width, height);
            damaged = false;
        }
        drawAvatars(g2, drawableAvatars);
        if (drawableAvatars.length > 0) {
            drawAvatarName(g2);
        }
        g2.setComposite(oldComposite);
    }

    private void drawAvatars(Graphics2D g2, DrawableAvatar[] drawableAvatars) {
        for (DrawableAvatar avatar : drawableAvatars) {
            AlphaComposite composite = AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, (float) avatar.getAlpha());
            g2.setComposite(composite);
            g2.drawImage(avatars.get(avatar.getIndex()).getImage(),
                    (int) avatar.getX(), (int) avatar.getY(),
                    avatar.getWidth(), avatar.getHeight(), null);
        }
    }

    private DrawableAvatar[] sortAvatarsByDepth(int x, int y,
            int width, int height) {
        List<DrawableAvatar> drawables = new LinkedList<DrawableAvatar>();
        for (int i = 0; i < avatars.size(); i++) {
            promoteAvatarToDrawable(drawables,
                    x, y, width, height, i - avatarIndex);
        }
        DrawableAvatar[] drawableAvatars = new DrawableAvatar[drawables.size()];
        drawableAvatars = drawables.toArray(drawableAvatars);
        Arrays.sort(drawableAvatars);
        return drawableAvatars;
    }

    private void drawAvatarName(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(titleFont);
        double x = (getWidth() - g2.getFontMetrics().stringWidth(avatarText)) / 2.0;
        double y = (getHeight() + CD_SIZE) / 2.0 + 100;
        g2.drawString(avatarText, (float) x, (float) y);
    }

    private void promoteAvatarToDrawable(List<DrawableAvatar> drawables,
            int x, int y, int width, int height,
            int offset) {

        double spacing = offset * avatarSpacing;
        double avatarPosition = this.avatarPosition + spacing;
        if (avatarIndex + offset < 0
                || avatarIndex + offset >= avatars.size()) {
            return;
        }
        int avatarWidth = displayWidth;
        int avatarHeight = displayHeight;
        double result = computeModifier(avatarPosition);
        int newWidth = (int) (avatarWidth * result);
        if (newWidth == 0) {
            return;
        }
        int newHeight = (int) (avatarHeight * result);
        if (newHeight == 0) {
            return;
        }
        double avatar_x = x + (width - newWidth) / 2.0;
        double avatar_y = y + (height - newHeight / 2.0) / 2.0;
        double semiWidth = width / 2.0;
        avatar_x += avatarPosition * semiWidth;
        if (avatar_x >= width || avatar_x < -newWidth) {
            return;
        }
        drawables.add(new DrawableAvatar(avatarIndex + offset,
                avatar_x, avatar_y,
                newWidth, newHeight,
                avatarPosition, result));
    }

    private void computeEquationParts() {
        exp_multiplier = Math.sqrt(2.0 * Math.PI) / sigma / rho;
        exp_member = 4.0 * sigma * sigma;
    }

    private double computeModifier(double x) {
        double result = computeModifierUnprotected(x);
        if (result > 1.0) {
            result = 1.0;
        } else if (result < -1.0) {
            result = -1.0;
        }
        return result;
    }

    private double computeModifierUnprotected(double x) {
        return exp_multiplier * Math.exp((-x * x) / exp_member);
    }

    private void addInputListeners() {
        addMouseListener(focusGrabber);
        addMouseListener(avatarScroller);
        addMouseListener(mouseAvatarSelector);
        addMouseMotionListener(cursorChanger);
        addMouseWheelListener(wheelScroller);
        addKeyListener(keyScroller);
        addKeyListener(keyAvatarSelector);
    }

    private void initInputListeners() {
        focusGrabber = new FocusGrabber();
        avatarScroller = new AvatarScroller();
        mouseAvatarSelector = new MouseAvatarSelector();
        cursorChanger = new CursorChanger();
        wheelScroller = new MouseWheelScroller();
        keyScroller = new KeyScroller();
        keyAvatarSelector = new KeyAvatarSelector();
    }

    private void setAvatarIndex(int index) {
        avatarIndex = index;
        avatarText = avatars.get(index).getLabel();
        notifyListSelectionListener();
    }

    private void scrollBy(int increment) {
        setAvatarIndex(avatarIndex + increment);
        if (avatarIndex < 0) {
            setAvatarIndex(0);
        } else if (avatarIndex >= avatars.size()) {
            setAvatarIndex(avatars.size() - 1);
        }
        damaged = true;
        repaint();
    }

    private void scrollAndAnimateBy(int increment) {
        if ((scrollerTimer == null || !scrollerTimer.isRunning())) {
            int index = avatarIndex + increment;
            if (index < 0) {
                index = 0;
            } else if (index >= avatars.size()) {
                index = avatars.size() - 1;
            }
            DrawableAvatar drawable = null;
            if (drawableAvatars != null) {
                for (DrawableAvatar avatar : drawableAvatars) {
                    if (avatar.index == index) {
                        drawable = avatar;
                        break;
                    }
                }
            }
            if (drawable != null) {
                scrollAndAnimate(drawable);
            }
        }
    }

    private void scrollAndAnimate(DrawableAvatar avatar) {
        scrollerTimer = new Timer(10, new AutoScroller(avatar));
        scrollerTimer.start();
    }

    private DrawableAvatar getHitAvatar(int x, int y) {
        for (DrawableAvatar avatar : drawableAvatars) {
            Rectangle hit = new Rectangle((int) avatar.getX(), (int) avatar.getY(),
                    avatar.getWidth(), avatar.getHeight() / 2);
            if (hit.contains(x, y)) {
                return avatar;
            }
        }
        return null;
    }

    private void startFader() {
        faderTimer = new Timer(10, new FaderAction());
        faderTimer.start();
    }

    //--------------------------------------------------------------------------
    public void addListSelectionListener(ListSelectionListener listener) {
        listSelectionListeners.add(listener);
    }

    public void removeListSelectionListener(ListSelectionListener listener) {
        listSelectionListeners.remove(listener);
    }

    private void notifyListSelectionListener() {
        ListSelectionEvent event = new ListSelectionEvent(
                this, avatarIndex, avatarIndex, false);
        for (ListSelectionListener listener : listSelectionListeners) {
            listener.valueChanged(event);
        }
    }

    public void addBookSelectionListener(BookSelectionListener listener) {
        panelListeners.add(listener);
    }

    //--------------------------------------------------------------------------
    private class FaderAction implements ActionListener {

        private FaderAction() {
            alphaLevel = 0.0f;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            alphaLevel += 0.025f;
            if (alphaLevel > 1.0f) {
                alphaLevel = 1.0f;
                faderTimer.stop();
            }
            repaint();
        }
    }

    private class DrawableAvatar implements Comparable {

        private int index;
        private double x;
        private double y;
        private int width;
        private int height;
        private double zOrder;
        private double position;

        private DrawableAvatar(int index,
                double x, double y, int width, int height,
                double position, double zOrder) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.position = position;
            this.zOrder = zOrder;
        }

        @Override
        public int compareTo(Object o) {
            double zOrder2 = ((DrawableAvatar) o).zOrder;
            if (zOrder < zOrder2) {
                return -1;
            } else if (zOrder > zOrder2) {
                return 1;
            }
            return 0;
        }

        public double getPosition() {
            return position;
        }

        public double getAlpha() {
            return zOrder * alphaLevel;
        }

        public int getHeight() {
            return height;
        }

        public int getIndex() {
            return index;
        }

        public int getWidth() {
            return width;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    private class MouseWheelScroller implements MouseWheelListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int increment = e.getWheelRotation();
            scrollAndAnimateBy(increment);
        }
    }

    private class KeyScroller extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_UP:
                    scrollAndAnimateBy(-1);
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_DOWN:
                    scrollAndAnimateBy(1);
                    break;
                case KeyEvent.VK_END:
                    scrollBy(avatars.size() - avatarIndex - 1);
                    break;
                case KeyEvent.VK_HOME:
                    scrollBy(-avatarIndex - 1);
                    break;
                case KeyEvent.VK_PAGE_UP:
                    scrollAndAnimateBy(-avatarAmount / 2);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    scrollAndAnimateBy(avatarAmount / 2);
                    break;
            }
        }
    }

    private class FocusGrabber extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            requestFocus();
        }
    }

    private class AvatarScroller extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if ((scrollerTimer != null && scrollerTimer.isRunning())
                    || drawableAvatars == null) {
                return;
            }

            if (e.getButton() == MouseEvent.BUTTON1) {
                DrawableAvatar avatar = getHitAvatar(e.getX(), e.getY());
                if (avatar != null && avatar.getIndex() != avatarIndex) {
                    scrollAndAnimate(avatar);
                }
            }
        }
    }

    private class DamageManager extends ComponentAdapter {

        @Override
        public void componentResized(ComponentEvent e) {
            damaged = true;
        }
    }

    private class AutoScroller implements ActionListener {

        private double position;
        private int index;
        private long start;

        private AutoScroller(DrawableAvatar avatar) {
            this.index = avatar.getIndex();
            this.position = avatar.getPosition();
            this.start = System.currentTimeMillis();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long elapsed = System.currentTimeMillis() - start;
            avatarText = avatars.get(index).getLabel();
            double newPosition = (elapsed / ANIM_SCROLL_DELAY) * -position;
            if (elapsed >= ANIM_SCROLL_DELAY) {
                ((Timer) e.getSource()).stop();
                setAvatarIndex(index);
                setPosition(0.0);
                return;
            }
            setPosition(newPosition);
        }
    }

    private class CursorChanger extends MouseMotionAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            if ((scrollerTimer != null && scrollerTimer.isRunning())
                    || drawableAvatars == null) {
                return;
            }
            DrawableAvatar avatar = getHitAvatar(e.getX(), e.getY());
            if (avatar != null) {
                getParent().setCursor(Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR));
            } else {
                getParent().setCursor(Cursor.getPredefinedCursor(
                        Cursor.DEFAULT_CURSOR));
            }
        }
    }

    private class KeyAvatarSelector extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if ((scrollerTimer == null || !scrollerTimer.isRunning())
                    && drawableAvatars != null) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    for (BookSelectionListener listener : panelListeners) {
                        listener.bookSelected(
                                avatars.get(avatarIndex).getBook());
                    }
                }
            }
        }
    }

    private class MouseAvatarSelector extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if ((scrollerTimer == null || !scrollerTimer.isRunning())
                    && drawableAvatars != null) {
                if (e.getButton() == MouseEvent.BUTTON1
                        && e.getClickCount() == 2) {
                    DrawableAvatar avatar = getHitAvatar(e.getX(), e.getY());
                    if (avatar != null) {
                        for (BookSelectionListener listener : panelListeners) {
                            listener.bookSelected(
                                    avatars.get(avatar.getIndex()).getBook());
                        }
                    }
                }
            }
        }
    }
}
