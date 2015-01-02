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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

public class Book {

    private int id;
    private int auflage;
    private long lfdNr;
    private float preis;
    private String sammlung;
    private String verlag;
    private String reihe;
    private String titelDeutsch;
    private String titelFremd;
    private String zyklus;
    private String autor;
    private String genre;
    private String format;
    private String zustand;
    private String jahr;
    private String ort;
    private String kommentar;
    private String titelBildName;
    private Date laetzteAenderung;
    private byte[] titelBild;
    public static final String SQL_FIELDS = "id,sammlung,verlag,reihe,lfdnr,"
            + "titel_deutsch,titel_fremd,zyklus,autor,genre,format,"
            + "auflage,jahr,zustand,preis,letzte_aenderung,ort,kommentar,"
            + "titelbild_name";

    public Book() {
    }

    public void stringOrNull(PreparedStatement st, int nr, String value)
            throws SQLException {
        if (value == null) {
            st.setNull(nr, Types.VARCHAR);
        } else if (value.trim().isEmpty()) {
            st.setNull(nr, Types.VARCHAR);
        } else {
            st.setString(nr, value.trim());
        }
    }

    public static Book getInstance(int id) throws Exception {
        PreparedStatement st = BookDatabase.getInstance().prepare(
                "SELECT " + SQL_FIELDS + " FROM buch WHERE id=?");
        st.setInt(1, id);
        ResultSet res = st.executeQuery();
        if (!res.next()) {
            throw new Exception("Kein Buch Nr. " + id + " vorhanden");
        }
        Book result = getInstance(res);
        st.close();
        return result;
    }

    public static Book getInstance(ResultSet res) throws Exception {
        
        Book book = new Book();
        book.setId(res.getInt(1));
        book.setSammlung(res.getString(2));
        book.setVerlag(res.getString(3));
        book.setReihe(res.getString(4));
        book.setLfdNr(res.getLong(5));
        book.setTitelDeutsch(res.getString(6));
        book.setTitelFremd(res.getString(7));
        book.setZyklus(res.getString(8));
        book.setAutor(res.getString(9));
        book.setGenre(res.getString(10));
        book.setFormat(res.getString(11));
        book.setAuflage(res.getInt(12));
        book.setJahr(res.getString(13));
        book.setZustand(res.getString(14));
        book.setPreis(res.getFloat(15));
        book.setLaetzteAenderung(new Date(res.getLong(16)));
        book.setOrt(res.getString(17));
        book.setKommentar(res.getString(18));
        book.titelBildName = res.getString(19);
        return book;
    }

    public void save() throws Exception {
        String sql;
        if (id == 0) {
//            PreparedStatement stcheck =
//                    BookDatabase.getInstance().prepare(
//                    "SELECT id FROM buch WHERE verlag=? AND lfdnr=?");
//            stcheck.setString(1, verlag);
//            stcheck.setLong(2, lfdNr);
//            ResultSet rescheck = stcheck.executeQuery();
//            if (rescheck.next()) {
//                rescheck.close();
//                throw new Exception("Das Buch existiert bereits!");
//            }
//            rescheck.close();
            sql = "INSERT INTO buch(sammlung,verlag,reihe,lfdnr,"
                    + "titel_deutsch,titel_fremd,zyklus,autor,genre,format,"
                    + "auflage,jahr,zustand,preis,letzte_aenderung,ort,"
                    + "kommentar,titelbild_name)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        } else {
            sql = "UPDATE buch SET sammlung=?,verlag=?,reihe=?,lfdnr=?,"
                    + "titel_deutsch=?,titel_fremd=?,zyklus=?,autor=?,genre=?,"
                    + "format=?,auflage=?,jahr=?,zustand=?,preis=?,"
                    + "letzte_aenderung=?,ort=?,kommentar=? "
                    + "WHERE id=?";
        }
        PreparedStatement st = BookDatabase.getInstance().prepare(sql);
        st.setString(1, sammlung);
        st.setString(2, verlag);
        stringOrNull(st, 3, reihe);
        st.setLong(4, lfdNr);
        stringOrNull(st, 5, titelDeutsch);
        stringOrNull(st, 6, titelFremd);
        stringOrNull(st, 7, zyklus);
        stringOrNull(st, 8, autor);
        st.setString(9, genre);
        st.setString(10, format);
        if (auflage > 0) {
            st.setInt(11, auflage);
        } else {
            st.setNull(11, Types.INTEGER);
        }
        stringOrNull(st, 12, jahr);
        stringOrNull(st, 13, zustand);
        if (preis > 0.0f) {
            st.setFloat(14, preis);
        } else {
            st.setNull(14, Types.DECIMAL);
        }
        st.setLong(15, new Date().getTime());
        st.setString(16, ort);
        st.setString(17, kommentar);

        // Eindeutigen Bildnamen bei Neuanlage vergeben
        if (id == 0) {
            titelBildName = Utils.randomHexString() + ".jpg";
            st.setString(18, titelBildName);
        } else {
            st.setInt(18, id);
        }

        if (st.executeUpdate() != 1) {
            throw new IllegalStateException("executeUpdate != 1");
        }
        if (id == 0) {
            ResultSet res = BookDatabase.getInstance().query(
                    "SELECT MAX(id) FROM buch");
            res.next();
            id = res.getInt(1);
            res.getStatement().close();
            res.close();
        }
        st.close();

        // Titelbild speichern
        Utils.bytesToFile(titelBild,
                BookDatabase.getInstance().getImageFile(titelBildName));
    }

    public void delete() throws Exception {
        if (id == 0) {
            throw new IllegalStateException();
        }
        BookDatabase db = BookDatabase.getInstance();
        db.update("DELETE FROM buch WHERE id=" + id);
        db.getImageFile(titelBildName).delete();
        titelBildName = null;
        id = 0;
    }

    public int getAuflage() {
        return auflage;
    }

    public void setAuflage(int auflage) {
        this.auflage = auflage;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJahr() {
        return jahr;
    }

    public void setJahr(String jahr) {
        this.jahr = jahr;
    }

    public String getKommentar() {
        return kommentar;
    }

    public void setKommentar(String kommentar) {
        this.kommentar = kommentar;
    }

    public Date getLaetzteAenderung() {
        return laetzteAenderung;
    }

    public void setLaetzteAenderung(Date laetzteAenderung) {
        this.laetzteAenderung = laetzteAenderung;
    }

    public long getLfdNr() {
        return lfdNr;
    }

    public void setLfdNr(long lfdNr) {
        this.lfdNr = lfdNr;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public float getPreis() {
        return preis;
    }

    public void setPreis(float preis) {
        this.preis = preis;
    }

    public String getReihe() {
        return reihe;
    }

    public void setReihe(String reihe) {
        this.reihe = reihe;
    }

    public String getSammlung() {
        return sammlung;
    }

    public void setSammlung(String sammlung) {
        this.sammlung = sammlung;
    }

    public byte[] getTitelBild() throws Exception {
        if (titelBild == null) {
            titelBild = Utils.bytesFromFile(
                    BookDatabase.getInstance().getImageFile(titelBildName));
        }
        return titelBild;
    }

    public void setTitelBild(byte[] titelBild) {
        this.titelBild = titelBild;
    }

    public String getTitelDeutsch() {
        return titelDeutsch;
    }

    public void setTitelDeutsch(String titelDeutsch) {
        this.titelDeutsch = titelDeutsch;
    }

    public String getTitelFremd() {
        return titelFremd;
    }

    public void setTitelFremd(String titelFremd) {
        this.titelFremd = titelFremd;
    }

    public String getVerlag() {
        return verlag;
    }

    public void setVerlag(String verlag) {
        this.verlag = verlag;
    }

    public String getZustand() {
        return zustand;
    }

    public void setZustand(String zustand) {
        this.zustand = zustand;
    }

    public String getZyklus() {
        return zyklus;
    }

    public void setZyklus(String zyklus) {
        this.zyklus = zyklus;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Book) {
            Book other = (Book) obj;
            return this.id == other.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.id;
        return hash;
    }
    
}
