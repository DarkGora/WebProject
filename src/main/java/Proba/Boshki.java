package Proba;

import java.io.FileWriter;
import java.io.IOException;

public class Boshki {


    public static void main(String[] args) throws IOException {
        Person p = new Person(7, "Sed", "pulia");
        try (FileWriter fw = new FileWriter("Studik.txt")) {
            fw.write(p.toString());
            fw.flush();
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
