package org.example.Proba;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class Bolik {
    public static void main(String[] args) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("Studik.ser"))) {
            List<Person> Studik = (List)ois.readObject();
            System.out.println(Studik);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
