package Proba;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Studik {
    public static void main(String[] args) {
        List<Person> Pablos = new ArrayList<>();
        Pablos.add(new Person(1, "Pablo", "Pull"));
        Pablos.add(new Person(2, "Pablo", "Koll"));
        Pablos.add(new Person(3, "Pablo", "Bolt"));
        Pablos.add(new Person(4, "Pablo", "Bol"));
        Pablos.add(new Person(5, "Pablo", "Good"));
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("Studik.ser"))) {
            oos.writeObject(Pablos);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}



