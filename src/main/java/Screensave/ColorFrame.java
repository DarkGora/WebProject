package Screensave;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.awt.*;
import java.util.Random;


public abstract class ColorFrame extends JFrame {

    public ColorFrame() throws HeadlessException {
        setSize(100, 200);
        setVisible(true);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void showOnRandomPlace() {
        Random rand = new Random();
        setLocation(rand.nextInt(1920), rand.nextInt(1080));
        getContentPane().setBackground(getColor());
        repaint();
    }

    protected abstract Color getColor();
}

