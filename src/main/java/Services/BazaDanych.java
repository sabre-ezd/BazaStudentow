package Services;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.ArrayList;

import Exceptions.InvalidPESELException;
import Models.Dziekanat;
import Models.Osoba;
import Models.Prowadzacy;
import Models.Student;
import Utils.Passwords;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.jetbrains.annotations.NotNull;

public final class BazaDanych {
    public static BazaDanych bazaDanych = new BazaDanych();
    private Connection conn;
    private PreparedStatement ps;

    private BazaDanych() throws SQLException {
        Connection conn = null;
        String path = "jdbc:sqlite:baza.db";
        ps = conn.prepareStatement("Select * from studenci");
        ps.closeOnCompletion();
        try {
            conn = DriverManager.getConnection(path);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        if (conn != null)
            this.conn = conn;
    }

    /**
     * Destruktor bazy danych
     */
    protected void finalize() {
        if (conn != null) {
            try {
                conn.commit();
                conn.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    private void clearPreparedStatement() {

    }

    public ResultSet getStudents() throws SQLException {
        ps = conn.prepareStatement("SELECT * FROM studenci");
        return ps.executeQuery();
    }

    public ResultSet getProwadzacy() throws SQLException {
        ps = conn.prepareStatement("SELECT * FROM prowadzacy");
        return ps.executeQuery();
    }

    public ResultSet getPrzedmioty() throws SQLException {
        ps = conn.prepareStatement("SELECT * FROM przedmioty");
        return ps.executeQuery();
    }

    public Osoba logIn(String imienazwisko, @NotNull String haslo, String pozycja) throws SQLException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidPESELException {
        String query = String.format("SELECT * FROM %s WHERE (imienazwisko = ?)", pozycja);
        ps = conn.prepareStatement(query);
        ps.setString(1, imienazwisko);
        ResultSet result = ps.executeQuery();
        if (!result.next())
            return null;
        String hash = result.getString("passwordhash");
        byte[] salt = result.getBytes("salt");
        int id = result.getInt("id");
        if (Passwords.validatePassword(haslo, salt, hash)) {
            switch (pozycja) {
                case "studenci":
                    return Student.createStudent(result);
                case "prowadzacy":
                    return Prowadzacy.createProwadzacy(result);
                case "dziekanat":
                    return Dziekanat.createDziekanat(result);
                default:
                    return null;
            }
        }
        return null;
    }


    public void addStudent(Student s, char[] haslo) throws SQLException, InvalidKeySpecException, NoSuchAlgorithmException {
        ps = conn.prepareStatement("INSERT INTO studenci(imienazwisko, passwordhash, " +
                "salt, pesel, rokstudiow, nralbumu) VALUES (?, ?, ?, ?, ?, ?)");
        ImmutablePair<String, byte[]> hasla = Passwords.generateHashPair(haslo);
        bindStudentFields(s, hasla);
        boolean result = ps.execute();
        conn.commit();
    }

    public boolean addProwadzacy(Prowadzacy p, char[] haslo) throws SQLException, InvalidKeySpecException, NoSuchAlgorithmException {
        ps = conn.prepareStatement("INSERT INTO prowadzacy(imienazwisko, passwordhash, przedmiot, salt) VALUES (?, ?, ?, ?)", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        ImmutablePair<String, byte[]> hasla = Passwords.generateHashPair(haslo);
        ps.setString(1, p.getImienazwisko());
        ps.setString(2, hasla.left);
        ps.setString(3, p.getPrzedmiot());
        ps.setBytes(4, hasla.right);
        boolean result = ps.execute();
        conn.commit();
        return result;
    }

    public boolean addDziekanat(Dziekanat dziekanat, char[] haslo) throws SQLException, InvalidKeySpecException, NoSuchAlgorithmException {
        ps = conn.prepareStatement("INSERT INTO dziekanat(imienazwisko, passwordhash, salt) VALUES (?,?,?)");
        @NotNull ImmutablePair<String, byte[]> hasla = Passwords.generateHashPair(haslo);
        ps.setString(1, dziekanat.getImienazwisko());
        ps.setString(2, hasla.left);
        ps.setBytes(3, hasla.right);
        return ps.execute();
    }

    public ResultSet getStudent(Student s) throws SQLException {
        // TODO: jezeli nie wszystkie pola studenta sa podane, dodaj dwiazdki (*)
        ps = conn.prepareStatement("SELECT * FROM studenci WHERE (?=?)");
        if (s.getNralbumu() != 0) {
            ps.setString(1, "id");
            ps.setInt(2, s.getNralbumu());
        } else if (!s.getImieNazwisko().equals(""))
            ps.setString(1, "imienazwisko");
            ps.setString(2, s.getImieNazwisko());
        return ps.executeQuery();
    }

    public void editStudent (Student s, char[] haslo) throws SQLException, InvalidKeySpecException, NoSuchAlgorithmException {
        ImmutablePair<String, byte[]> hashPair = Passwords.generateHashPair(haslo);
        ps = conn.prepareStatement("UPDATE studenci SET imienazwisko = ?, passwordhash = ?, salt = ?, pesel = ?, rokstudiow = ?, nralbumu = ? WHERE id = ?");
        bindStudentFields(s, hashPair);
        ps.setInt(7,s.getId());
    }

    private void bindStudentFields(Student s, ImmutablePair<String, byte[]> hashPair) throws SQLException {
        ps.setString(1, s.getImieNazwisko());
        ps.setString(2, hashPair.left);
        ps.setBytes(3,hashPair.right);
        ps.setString(4,s.getPesel());
        ps.setInt(5,s.getRok_studiow());
        ps.setInt(6,s.getNralbumu());
    }

    public void updateGrades(int studentId, String grades, String ocenakoncowa, String przedmiotDb) throws SQLException {
        String query = "UPDATE " + przedmiotDb;
        ps = conn.prepareStatement(query + " SET oceny ?, ocenakoncowa = ? WHERE id_stud = ?");
        ps.setString(1, grades);
        ps.setString(2, ocenakoncowa);
        ps.setInt(3, studentId);
        ps.execute();
    }

    public ArrayList<ImmutablePair<String, ResultSet>> getGrades(int student_id) throws SQLException {
        ps = conn.prepareStatement("SELECT * FROM przedmioty");
        ResultSet przedmioty = ps.executeQuery();
        ArrayList<ImmutablePair<String, ResultSet>> results = new ArrayList<>();
        while (przedmioty.next()) {
            String przedmiot = przedmioty.getString("nazwatabeli");
            System.out.println(przedmiot);
            String query = String.format("SELECT * FROM %s WHERE (id_stud = ?)", przedmiot);
            ps = conn.prepareStatement(query);
            ps.setInt(1, student_id);
            ResultSet oceny = ps.executeQuery();
            results.add(new ImmutablePair<>(przedmioty.getString("nazwa"), oceny));
        }
        return results;
    }

    public ResultSet getGrade(int student_id, String przedmiot) throws SQLException {
        String query = String.format("SELECT * FROM %S WHERE (id_stud = ?)", przedmiot);
        ps = conn.prepareStatement(query);
        ps.setInt(1, student_id);
        return ps.executeQuery();
    }

    public ArrayList<Integer> getStudentIDList(String przedmiot) throws SQLException {
        String query = String.format("SELECT * FROM %s", przedmiot);
        ps = conn.prepareStatement(query);
        ArrayList<Integer> out = new ArrayList<>();
        ResultSet resultSet = ps.executeQuery();
        while (resultSet.next())
            out.add(resultSet.getInt("id_stud"));
        return out;
    }

    public ArrayList<ImmutableTriple<String, String, String>> getGradeList(String przedmiot) throws SQLException {
        String query = String.format("SELECT * FROM %s", przedmiot);
        ps = conn.prepareStatement(query);
        ArrayList<ImmutableTriple<String, String, String>> out = new ArrayList<>();
        ResultSet resultSet = ps.executeQuery();
        ResultSet uczniowie;
        while (resultSet.next()) {
            ps = conn.prepareStatement("SELECT * FROM studenci WHERE id=?");
            ps.setInt(1, resultSet.getInt("id_stud"));
            uczniowie = ps.executeQuery();
            uczniowie.next();
            out.add(new ImmutableTriple<>(uczniowie.getString("imienazwisko"), resultSet.getString("oceny"), resultSet.getString("ocenakoncowa")));
        }
        return out;
    }

    public void addPrzedmiot(String przedmiot) throws SQLException {
        String tabelanazwa = WordUtils.capitalizeFully(przedmiot, ' ').replaceAll(" ", "");
        String sql = String.format("create table %s\n" +
                "(\n" +
                "    id      INTEGER\n" +
                "        constraint %s\n" +
                "            primary key autoincrement,\n" +
                "    id_stud int not null\n" +
                "        references studenci\n" +
                "            on update cascade on delete cascade,\n" +
                "    oceny   text default null,\n" +
                "    ocenakoncowa text default null\n" +
                ");" +
                "create unique index %s_id_stud_uindex\n" +
                "    on %s (id_stud);\n", tabelanazwa, (tabelanazwa + "_pk"), (tabelanazwa), tabelanazwa);
        ps = conn.prepareStatement(sql);
        ps.execute();
        ps = conn.prepareStatement("INSERT INTO przedmioty(nazwa, nazwatabeli) VALUES (?, ?)");
        ps.setString(1, przedmiot);
        ps.setString(2, tabelanazwa);
        ps.execute();
        conn.commit();
    }

    public void removeStudent(@NotNull ResultSet student) throws SQLException {
        student.deleteRow();
        conn.commit();
    }

    public void main(String[] args) {
//        ImmutablePair<String, byte[]> hashPair = Passwords.generateHashPair("[1, 2, 3]");
//        ps = conn.prepareStatement("UPDATE studenci SET passwordhash = ?, salt = ? WHERE imienazwisko = 'Jan Kowalski'");
//        ps.setString(1, hashPair.left);
//        ps.setBytes(2, hashPair.right);
//        ps.execute();
    }

}
