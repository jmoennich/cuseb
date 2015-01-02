/*  
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
package de.cuseb.data;

import de.cuseb.Utils;
import de.cuseb.dialogs.FileCopyDialog;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JOptionPane;

public class BookDatabase {

    public static final String AMAZON_SECRET_KEY = "amazon_secret_key";
    public static final String AMAZON_ACCESS_KEY_ID = "amazon_access_key_id";
    public static final String AMAZON_ASSOCIATE_TAG = "amazon_associate_tag";
    public static final String BACKUP_LAST_DATE = "backup_last_date";
    public static final String BACKUP_LOCATION = "backup_location";
    public static final String DEFAULT_COLUMNS = "default_columns";
    public static final String MAIL_SMTP_TO = "mail_smtp_to";
    public static final String MAIL_SMTP_HOST = "mail_smtp_host";
    public static final String MAIL_SMTP_PASSWORD = "mail_smtp_password";
    public static final String SCAN_COMMAND = "scan_command";
    public static final String SCAN_CROP_THRESHOLD = "scan_crop_threshold";
    private static final String DB_FILENAME = "cuseb.db";
    private static BookDatabase instance;
    private HashMap<String, String> configValues;
    private Connection connection;
    private File imagesDir;
    private File baseDir;
    private File dbFile;

    private BookDatabase() throws Exception {

        // Verzeichnisse evt. anlegen
        File baseDir = getBaseDir();
        if (!baseDir.exists()) {
            baseDir.mkdir();
        }
        File imgsDir = getImagesDir();
        if (!imgsDir.exists()) {
            imgsDir.mkdir();
        }
        Utils.createImageDirs(imagesDir);

        // SQLite Datenbank im Home-Verzeichnis
        dbFile = new File(getBaseDir(), DB_FILENAME);
        boolean dbExisted = dbFile.exists();

        // Verbindung herstellen
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + dbFile.toString());
        Statement st = connection.createStatement();
        st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sammlung("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name VARCHAR(64))");
        st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS buch("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "sammlung VARCHAR(32) NOT NULL,"
                + "verlag VARCHAR(64) NOT NULL,"
                + "reihe VARCHAR(64),"
                + "lfdnr LONG,"
                + "titel_deutsch VARCHAR(128),"
                + "titel_fremd VARCHAR(128),"
                + "zyklus VARCHAR(64),"
                + "autor VARCHAR(64),"
                + "genre CHAR(2),"
                + "format CHAR(2),"
                + "auflage INTEGER,"
                + "jahr VARCHAR(10),"
                + "zustand CHAR(4),"
                + "preis DECIMAL(5,2),"
                + "letzte_aenderung LONG,"
                + "ort VARCHAR(32),"
                + "kommentar VARCHAR(64),"
                + "titelbild_name)");
        st.executeUpdate("CREATE INDEX IF NOT EXISTS index_sammlung "
                + "ON buch(sammlung)");
        st.executeUpdate("CREATE INDEX IF NOT EXISTS index_verlag "
                + "ON buch(verlag)");
        st.executeUpdate("CREATE INDEX IF NOT EXISTS index_reihe "
                + "ON buch(reihe)");
        st.executeUpdate("CREATE INDEX IF NOT EXISTS index_autor "
                + "ON buch(autor)");
        st.executeUpdate("CREATE INDEX IF NOT EXISTS index_ort "
                + "ON buch(ort)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS konfiguration("
                + "schluessel VARCHAR(64) PRIMARY KEY,"
                + "wert VARCHAR(256))");
        // Meldung nach Neuanlegen der DB anzeigen
        if (!dbExisted) {
            JOptionPane.showMessageDialog(null,
                    "Die folgende Datenbank wurde angelegt:\n"
                    + dbFile.toString(),
                    "CuseB",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        backupCheck();
    }

    public static BookDatabase getInstance() {
        if (instance == null) {
            try {
                instance = new BookDatabase();
            } catch (Exception e) {
                Utils.handleError(e);
            }
        }
        return instance;
    }

    public float getSizeMB() {
        return (float) dbFile.length() / 1048576f;
    }

    public final File getBaseDir() {
        if (baseDir == null) {
            baseDir = new File(System.getProperty("user.home"), "cuseb");
        }
        return baseDir;
    }

    public final File getImagesDir() {
        if (imagesDir == null) {
            imagesDir = new File(getBaseDir(), "images");
        }
        return imagesDir;
    }

    public File getBackupDir() {
        for (File root : File.listRoots()) {
            File backupDir = new File(root, "cuseb");
            if (backupDir.exists()) {
                return backupDir;
            }
        }
        return null;
    }

    public final File getImageFile(String filename) {
        return getImageFile(filename, getImagesDir());
    }

    public final File getImageFile(String filename, File base) {
        return new File(new File(base, filename.substring(0, 1)), filename);
    }

    public void backup() throws Exception {

        ArrayList<File> copyFroms = new ArrayList<File>();
        ArrayList<File> copyTos = new ArrayList<File>();

        // Backup-Verzeichnis suchen
        File backupDir = getBackupDir();
        if (backupDir == null) {
            throw new Exception("Backup-Festplatte nicht gefunden!");
        }

        // Datenbank immer kopieren
        copyFroms.add(new File(getBaseDir(), DB_FILENAME));
        copyTos.add(new File(backupDir, DB_FILENAME));

        // Letztes Backup-Datum ermitteln
        long last = 0;
        try {
            last = Long.parseLong(getConfigValue(BACKUP_LAST_DATE));
        } catch (NumberFormatException e) {
        }

        // Bilder-Ordner auf Backup-Laufwerk anlegen
        File backupImagesDir = new File(backupDir, "images");
        if (!backupImagesDir.exists()) {
            backupImagesDir.mkdir();
        }
        Utils.createImageDirs(backupImagesDir);

        // Nur Bilder von geänderten Büchern kopieren
        ResultSet res = query("SELECT titelbild_name FROM buch "
                + "WHERE letzte_aenderung >= " + last);
        while (res.next()) {
            File fileFrom = getImageFile(res.getString(1));
            File fileTo = getImageFile(res.getString(1), backupImagesDir);
            if (fileFrom.exists()) {
                copyFroms.add(fileFrom);
                copyTos.add(fileTo);
            }
        }

        // Dateien kopieren
        FileCopyDialog dialog = new FileCopyDialog(null, copyFroms, copyTos);
        dialog.setVisible(true);

        // Kopierstatus anzeigen
        if (dialog.getException() != null) {
            JOptionPane.showMessageDialog(null,
                    "Die Sicherung folgender Dateien schlug fehl:\n"
                    + dialog.getException().getLocalizedMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null,
                    "Die Sicherung war erfolgreich!",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE);

            // Letztes Backup-Datum aktualisieren
            try {
                setConfigValue(BACKUP_LAST_DATE,
                        Long.toString(new Date().getTime()));
            } catch (Exception e) {
                Utils.handleError(e);
            }
        }
    }

    public final void backupCheck() {
        if (getConfigValue(BACKUP_LOCATION).isEmpty()) {
            return;
        }
        boolean askForBackup = false;
        String backupLast = getConfigValue(BACKUP_LAST_DATE);
        if ("".equals(backupLast)) {
            askForBackup = true;
        } else {
            long last = Long.parseLong(backupLast);
            long now = new Date().getTime();
            askForBackup = last < (now - 1000 * 60 * 60 * 24 * 7);
        }
        if (askForBackup) {
            if (JOptionPane.showConfirmDialog(null,
                    "Die letzte Sicherung liegt mehr als 7 Tage zurück.\n"
                    + "Soll nun eine Sicherung durchgeführt werden?",
                    "CuseB Sicherung",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                try {
                    backup();
                } catch (Exception e) {
                    Utils.handleError(e);
                }
            }
        }
    }

    public PreparedStatement prepare(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public ResultSet query(String sql) throws SQLException {
        Statement st = connection.createStatement();
        return st.executeQuery(sql);
    }

    public int update(String sql) throws SQLException {
        int rows;
        Statement st = connection.createStatement();
        rows = st.executeUpdate(sql);
        return rows;
    }

    public String getConfigValue(String key) {
        return getConfigValues().get(key);
    }

    public HashMap<String, String> getConfigValues() {
        if (configValues == null) {
            configValues = new HashMap<String, String>();
            try {
                ResultSet res = query(
                        "SELECT schluessel,wert FROM konfiguration");
                while (res.next()) {
                    configValues.put(res.getString(1), res.getString(2));
                }
                res.getStatement().close();
            } catch (Exception e) {
                Utils.handleError(e);
            }
            // Standardwerte
            setConfigDefaultValue(AMAZON_ACCESS_KEY_ID, "");
            setConfigDefaultValue(AMAZON_ASSOCIATE_TAG, "");
            setConfigDefaultValue(AMAZON_SECRET_KEY, "");
            setConfigDefaultValue(BACKUP_LAST_DATE, "");
            setConfigDefaultValue(BACKUP_LOCATION, "");
            setConfigDefaultValue(DEFAULT_COLUMNS, "Lfd Nr.,Autor,Titel");
            setConfigDefaultValue(MAIL_SMTP_HOST, "");
            setConfigDefaultValue(MAIL_SMTP_PASSWORD, "");
            setConfigDefaultValue(MAIL_SMTP_TO, "webmaster@cuseb.de");
            setConfigDefaultValue(SCAN_COMMAND, "");
            setConfigDefaultValue(SCAN_CROP_THRESHOLD, "0.25");
        }
        return configValues;
    }

    private void setConfigDefaultValue(String key, String value) {
        if (!configValues.containsKey(key)) {
            configValues.put(key, value);
        }
    }

    public void setConfigValue(String key, String value) throws Exception {
        PreparedStatement st = prepare(
                "INSERT OR REPLACE INTO konfiguration("
                + "schluessel,wert) VALUES (?,?)");
        st.setString(1, key);
        st.setString(2, value);
        st.executeUpdate();
        st.close();
        configValues.put(key, value);
    }

    public void setConfigValues(HashMap<String, String> values) throws Exception {
        PreparedStatement st = prepare(
                "INSERT OR REPLACE INTO konfiguration("
                + "schluessel,wert) VALUES (?,?)");
        for (String key : values.keySet()) {
            st.setString(1, key);
            st.setString(2, values.get(key));
            st.addBatch();
        }
        st.executeBatch();
        st.close();
        this.configValues = values;
    }

    //--------------------------------------------------------------------------
    public void migrateImages() throws Exception {

        // Verzeichnisse anlegen
        getBaseDir().mkdir();
        getImagesDir().mkdir();
        Utils.createImageDirs(getImagesDir());

        // Spalte hinzufügen
        update("ALTER TABLE buch ADD COLUMN titelbild_name CHAR(44)");

        // IDs aller Bücher holen
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ResultSet rs = query("SELECT id FROM buch");
        while (rs.next()) {
            ids.add(rs.getInt(1));
        }
        rs.getStatement().close();
        rs.close();

        for (int id : ids) {
            // Neuen Bildnamen erstellen und schreiben
            String filename = Utils.randomHexString() + ".jpg";
            update("UPDATE buch SET titelbild_name='"
                    + filename + "' WHERE id=" + id);

            // Bild in Datei schreiben
            ResultSet rsimg = query(
                    "SELECT titelbild FROM buch WHERE id=" + id);
            File out = getImageFile(filename);
            if (out.exists()) {
                throw new Exception("Collision!");
            }
            Utils.bytesToFile(rsimg.getBytes(1), out);
            rsimg.getStatement().close();
            rsimg.close();

            System.out.println("id: " + id
                    + ", filename: " + filename
                    + ", image-file: " + out);
        }

        update("UPDATE buch SET titelbild=NULL");
        update("VACUUM");
    }

    public static void main(String args[]) {
        try {
            //BookDatabase.getInstance().migrateImages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
