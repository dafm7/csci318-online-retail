package com.csci318.chatclient.presentation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageFormatterTest {

    @Test
    void convertsBoldAndListItems() {
        String html = MessageFormatter.toHtml("We have:\n- **Keyboard** $129.95\n* Mouse", 200);

        assertThat(html).contains("We have:<br>&bull; <b>Keyboard</b> $129.95<br>&bull; Mouse");
    }

    @Test
    void escapesMarkupInMessages() {
        String html = MessageFormatter.toHtml("<img src=x> & more", 200);

        assertThat(html).contains("&lt;img src=x&gt; &amp; more").doesNotContain("<img");
    }
}
