package Proba;

import java.io.Serializable;

public class Person implements Serializable {
    private int id;
    private String name;
    transient private String clothes;

    public Person(int id, String name, String clothes) {
        this.id = id;
        this.name = name;
        this.clothes = clothes;
    }

    @Override
    public String toString() {
        return "Person( id=" + id + ", name='" + name + ", clothes=" + clothes + '}';
    }
}