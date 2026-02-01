import javax.swing.*;
import java.awt.*;

public class showOptions {
    public static void showOptions() {
        initOptionsUI();

    }

    public static void initOptionsUI() {
        JFrame frame = new JFrame("Options");
        frame.setLayout(new FlowLayout());
        JCheckBox c1 = new  JCheckBox("Return manga titles in Japanese");
        JCheckBox c2 = new JCheckBox("Return ratings"); // TODO: ADD PANEL FOR STATS (GET /statistics/manga/{uuid})
    }
}
