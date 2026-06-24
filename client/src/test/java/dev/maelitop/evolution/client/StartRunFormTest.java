package dev.maelitop.evolution.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StartRunFormTest {

  @Test
  void parsesDefaults() {
    StartRunForm form = new StartRunForm();

    assertThat(form.seed()).isEqualTo(42L);
    assertThat(form.generations()).isEqualTo(100);
    assertThat(form.carnivores()).isZero();
  }

  @Test
  void editsActiveFieldOnly() {
    StartRunForm form = new StartRunForm();

    form.backspace();
    form.backspace();
    form.typeDigit(7);

    assertThat(form.seed()).isEqualTo(7L);
    assertThat(form.generations()).isEqualTo(100);
  }

  @Test
  void fieldNavigationCycles() {
    StartRunForm form = new StartRunForm();

    assertThat(form.field()).isEqualTo(StartRunForm.Field.SEED);
    form.nextField();
    assertThat(form.field()).isEqualTo(StartRunForm.Field.GENERATIONS);
    form.prevField();
    form.prevField();
    assertThat(form.field()).isEqualTo(StartRunForm.Field.CARNIVORES);
  }

  @Test
  void clampsEmptyGenerationsToOne() {
    StartRunForm form = new StartRunForm();
    form.nextField();

    form.backspace();
    form.backspace();
    form.backspace();

    assertThat(form.generations()).isEqualTo(1);
  }

  @Test
  void toggleOpensAndCloses() {
    StartRunForm form = new StartRunForm();

    assertThat(form.open()).isFalse();
    form.toggle();
    assertThat(form.open()).isTrue();
    form.close();
    assertThat(form.open()).isFalse();
  }
}
