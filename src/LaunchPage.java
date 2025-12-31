import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class LaunchPage extends JFrame implements ActionListener{
    JPanel headerPanel;
    JPanel selectionPanel;
    JPanel buttonGroup;
    JPanel controlPanel;
    JPanel navButton;
    JPanel gridPanel;
    JButton buttonPrev;
    JButton buttonNext;
    JComboBox<String> buttonMonth;
    JComboBox<String> buttonYear;
    JButton addEvent;
    JButton dayButtons;
    ImageIcon icon;

    public LaunchPage() {
        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        icon = new ImageIcon("src/resources/icon.png");
        this.setIconImage(icon.getImage());
        this.setTitle("Calendar App");

        // Header Panel setup
        headerPanel = new JPanel();    
        headerPanel.setPreferredSize(new Dimension(800, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10,10));
        headerPanel.setLayout(new BorderLayout());

        // Selection Panel setup
        selectionPanel = new JPanel(new BorderLayout());
        selectionPanel.setPreferredSize(new Dimension(200, 100));
        selectionPanel.setOpaque(false);
        
        // Button Group Panel setup
        buttonGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Button for Month
        buttonMonth = new JComboBox<>(new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"});
        buttonMonth.setFocusable(false);
        buttonGroup.setOpaque(false);
        buttonGroup.add(buttonMonth);

        // Button for Year
        buttonYear = new JComboBox<>(new String[]{"2022", "2023", "2024", "2025"});
        buttonYear.setFocusable(false); 
        buttonGroup.add(buttonYear);
        
        selectionPanel.add(buttonGroup, BorderLayout.SOUTH);
        headerPanel.add(selectionPanel, BorderLayout.WEST);

        // Control Panel setup
        controlPanel = new JPanel(new BorderLayout());
        controlPanel.setOpaque(false);
        
        // Navigation Buttons Panel setup
        navButton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        navButton.setOpaque(false);

        // Add Event Button
        addEvent = new JButton("Event");
        addEvent.setFocusable(false);
        navButton.add(addEvent);
        
        // Previous Buttons
        buttonPrev = new JButton("<");
        buttonPrev.setFocusable(false);
        navButton.add(buttonPrev);
        
        // Next Buttons
        buttonNext = new JButton(">");
        buttonNext.setFocusable(false);
        navButton.add(buttonNext);
        
        controlPanel.add(navButton, BorderLayout.SOUTH);
        headerPanel.add(controlPanel, BorderLayout.EAST);

        // Grid Setup
        gridPanel = new JPanel();
        gridPanel.setPreferredSize(new Dimension(400, 400));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10,10));
        gridPanel.setLayout(new GridLayout(7,5));
        for (int i = 1; i <= 35; i++) {
            dayButtons = new JButton(String.valueOf(i));
            dayButtons.setFocusable(false);
            gridPanel.add(dayButtons);
        }

        this.add(headerPanel, BorderLayout.NORTH);
        this.add(gridPanel, BorderLayout.CENTER);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        // Handle action events here
    }
}
