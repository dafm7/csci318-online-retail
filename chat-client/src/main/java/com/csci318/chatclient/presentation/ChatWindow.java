package com.csci318.chatclient.presentation;

import com.csci318.chatclient.service.ChatSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.Consumer;

// The chat window: an account bar at the top, the conversation in the middle,
// suggested questions and a message box at the bottom.
public class ChatWindow extends JFrame {

    private static final Color ACCENT = new Color(0x2557d6);
    private static final Color BOT_BUBBLE = new Color(0xeef1f6);
    private static final String[] SUGGESTIONS = {
            "Do you sell keyboards?", "What's in Electronics?", "Where is my latest order?"
    };

    private final ChatSession session;
    private final JPanel messages = new JPanel();
    private final JScrollPane scroll;
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JLabel accountLabel = new JLabel();
    private final JButton signInButton = new JButton("Sign in");
    private final JButton signOutButton = new JButton("Sign out");
    private final JPanel suggestions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

    public ChatWindow(ChatSession session) {
        super("CSCI318 Shop Assistant");
        this.session = session;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(440, 640);
        setLocationByPlatform(true);

        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        messages.setBorder(new EmptyBorder(8, 8, 8, 8));
        messages.setBackground(Color.WHITE);
        JPanel stack = new JPanel(new BorderLayout());   // keeps bubbles at the top instead of spread out
        stack.setBackground(Color.WHITE);
        stack.add(messages, BorderLayout.NORTH);
        scroll = new JScrollPane(stack);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(accountBar(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(inputArea(), BorderLayout.SOUTH);

        showAccount();
        addMessage(false, "Hi! I'm Shopbot. I can help you find products and check your orders. What are you looking for?");
        SwingUtilities.invokeLater(input::requestFocusInWindow);
    }

    // ---------- Layout ----------

    private JPanel accountBar() {
        JButton newChat = new JButton("New chat");
        newChat.addActionListener(e -> startNewChat("New conversation started. How can I help?"));
        signInButton.addActionListener(e -> showSignInDialog());
        signOutButton.addActionListener(e -> {
            runInBackground(() -> { session.signOut(); return null; },
                    ignored -> { showAccount(); clearMessages("You're signed out. How can I help?"); });
        });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.add(accountLabel);
        bar.add(signInButton);
        bar.add(signOutButton);
        bar.add(newChat);
        return bar;
    }

    private JPanel inputArea() {
        for (String question : SUGGESTIONS) {
            JButton b = new JButton(question);
            b.setFont(b.getFont().deriveFont(11f));
            b.addActionListener(e -> send(question));
            suggestions.add(b);
        }
        input.addActionListener(e -> send(input.getText()));   // Enter sends
        sendButton.addActionListener(e -> send(input.getText()));

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(new EmptyBorder(6, 8, 8, 8));
        row.add(input, BorderLayout.CENTER);
        row.add(sendButton, BorderLayout.EAST);

        JPanel area = new JPanel(new BorderLayout());
        area.add(suggestions, BorderLayout.NORTH);
        area.add(row, BorderLayout.SOUTH);
        return area;
    }

    // ---------- Actions ----------

    private void send(String text) {
        String message = text.trim();
        if (message.isEmpty()) return;
        input.setText("");
        suggestions.setVisible(false);
        addMessage(true, message);
        JComponent typing = addMessage(false, "Shopbot is typing…");
        setBusy(true);
        runInBackground(() -> session.send(message), reply -> {
            messages.remove(typing);
            addMessage(false, reply);
            setBusy(false);
            input.requestFocusInWindow();
        });
    }

    private void showSignInDialog() {
        JTextField email = new JTextField(20);
        JPasswordField password = new JPasswordField(20);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Email"));
        form.add(email);
        form.add(new JLabel("Password"));
        form.add(password);
        int choice = JOptionPane.showConfirmDialog(this, form, "Sign in", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        String pw = new String(password.getPassword());
        runInBackground(() -> session.signIn(email.getText().trim(), pw), (Optional<String> error) -> {
            if (error.isPresent()) {
                JOptionPane.showMessageDialog(this, error.get(), "Sign in", JOptionPane.ERROR_MESSAGE);
                return;
            }
            showAccount();
            addMessage(false, "You're signed in as " + session.customer().orElseThrow().name()
                    + ". Ask me about your orders any time.");
        });
    }

    private void startNewChat(String greeting) {
        runInBackground(() -> { session.newConversation(); return null; }, ignored -> clearMessages(greeting));
    }

    // Network calls run on a worker thread so the window never freezes; onDone runs back on the Swing thread
    private <T> void runInBackground(Supplier<T> work, Consumer<T> onDone) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() { return work.get(); }
            @Override protected void done() {
                try {
                    onDone.accept(get());
                } catch (Exception e) {
                    addMessage(false, "Something went wrong: " + e.getMessage());
                    setBusy(false);
                }
            }
        }.execute();
    }

    // ---------- View updates ----------

    private void showAccount() {
        var customer = session.customer();
        accountLabel.setText(customer.map(c -> "Hi, " + c.name()).orElse("Not signed in"));
        signInButton.setVisible(customer.isEmpty());
        signOutButton.setVisible(customer.isPresent());
    }

    private void setBusy(boolean busy) {
        input.setEnabled(!busy);
        sendButton.setEnabled(!busy);
    }

    private void clearMessages(String greeting) {
        messages.removeAll();
        suggestions.setVisible(true);
        addMessage(false, greeting);
    }

    private JComponent addMessage(boolean fromUser, String text) {
        JLabel bubble = new JLabel(MessageFormatter.toHtml(text, 260));
        bubble.setOpaque(true);
        bubble.setBackground(fromUser ? ACCENT : BOT_BUBBLE);
        bubble.setForeground(fromUser ? Color.WHITE : Color.DARK_GRAY);
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel row = new JPanel(new FlowLayout(fromUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 4)) {
            @Override public Dimension getMaximumSize() {   // stop BoxLayout stretching rows vertically
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.add(bubble);
        messages.add(row);
        messages.revalidate();
        messages.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
        return row;
    }
}
