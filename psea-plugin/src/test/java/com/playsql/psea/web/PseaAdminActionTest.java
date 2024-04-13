package com.playsql.psea.web;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PseaAdminActionTest {

    PseaAdminAction action = new PseaAdminAction();

    @Test
    public void testConversions() {
        assertThat(action.convertSizeToMachine("0"), is(0L));
        assertThat(action.convertSizeToMachine("1"), is(1L));
        assertThat(action.convertSizeToMachine("270"), is(270L));
        assertThat(action.convertSizeToMachine("1KB"), is(1000L));
        assertThat(action.convertSizeToMachine("10K"), is(10000L));
        assertThat(action.convertSizeToMachine("100K"), is(100000L));
        assertThat(action.convertSizeToMachine("100 KB"), is(100000L));
        assertThat(action.convertSizeToMachine("1M"), is(1000000L));
        assertThat(action.convertSizeToMachine("1G"), is(1000000000L));
    }

    @Test
    public void testFormatting() {
        assertThat(action.convertSizeToHuman(0L), is("0"));
        assertThat(action.convertSizeToHuman(1L), is("1"));
        assertThat(action.convertSizeToHuman(270L), is("270"));
        assertThat(action.convertSizeToHuman(1000L), is("1 K"));
        assertThat(action.convertSizeToHuman(10000L), is("10 K"));
        assertThat(action.convertSizeToHuman(100000L), is("100 K"));
        assertThat(action.convertSizeToHuman(1000000L), is("1 M"));
        assertThat(action.convertSizeToHuman(1000000000L), is("1 G"));
    }
}
