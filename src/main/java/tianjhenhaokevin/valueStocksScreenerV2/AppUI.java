package tianjhenhaokevin.valueStocksScreenerV2;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;

public class AppUI {
    private static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Value Stocks Screener V2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(false);

        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        PrintStream printStream = new PrintStream(new CustomOutputStream(textArea));
        JPanel inputPanel = new JPanel();

        JTextField inputField = new JTextField(50);
        inputPanel.add(inputField);

        JButton submitButton = new JButton("Submit");

        App app = new App(inputField, submitButton, printStream);

        submitButton.setEnabled(false);
        submitButton.addActionListener(e -> app.inputReady());
        inputPanel.add(submitButton);

        JScrollPane jScrollPane = new JScrollPane(textArea);
        jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        frame.add(jScrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        CompletableFuture.runAsync(app);
    }
}