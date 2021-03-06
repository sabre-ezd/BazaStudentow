package GUI;

import Models.Student;
import Services.BazaDanych;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Okno prezentujące wyniki wyszukiwania studentów do dodania do grupy.
 */
public class AddStudentsToClass extends JDialog {
    /**
     * Panel z wynikami wyszukiwania
     */
    SearchStudents wyniki;
    JButton dodaj;

    /**
     * @param przedmiot - nazwa przedmiotu, do którego listy obecności dodani będą studenci
     * @throws SQLException - generyczny błąd bazy danych
     */
    public AddStudentsToClass(String przedmiot) throws SQLException {
        this.setTitle("Wyniki wyszukiwania");
        this.setModal(true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.add(this.wyniki = new SearchStudents());
        this.setSize(470, 510);
        this.add(this.dodaj = new JButton("Dodaj"));
        this.setLayout(new FlowLayout());
        this.dodaj.addActionListener(e -> {
            int row = this.wyniki.tabelka.table.getSelectedRow();
            int id = Integer.parseInt(String.valueOf(this.wyniki.tabelka.table.getValueAt(row, 0)));
            Student temp = new Student();
            temp.setId(id);
            try {
                BazaDanych.bazaDanych.addStudentToClass(temp, przedmiot);
                this.dispose();
            } catch (SQLException throwables) {
                JOptionPane.showMessageDialog(null, ("Błąd: " + throwables.getMessage()));
                throwables.printStackTrace();
            }
        });
        this.setVisible(true);
    }
}
