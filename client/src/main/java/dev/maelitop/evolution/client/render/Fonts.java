package dev.maelitop.evolution.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * FreeType-baked fonts. Glyphs are baked at the back-buffer pixel density and drawn scaled down to
 * the logical size, so text stays crisp on HiDPI displays while layout math stays in logical
 * points.
 */
public final class Fonts {

  private static final String MONO_REGULAR = "fonts/JetBrainsMono-Regular.ttf";
  private static final String MONO_SEMIBOLD = "fonts/JetBrainsMono-SemiBold.ttf";
  private static final String SANS_SEMIBOLD = "fonts/Inter-SemiBold.ttf";
  private static final String SANS_BOLD = "fonts/Inter-Bold.ttf";

  public final BitmapFont mono10;
  public final BitmapFont mono11;
  public final BitmapFont mono12;
  public final BitmapFont mono13;
  public final BitmapFont mono14;

  public final BitmapFont sansDisplay;
  public final BitmapFont sansTitle;
  public final BitmapFont sansWordmark;
  public final BitmapFont sansButton;

  private final GlyphLayout layout = new GlyphLayout();
  private final float density;

  public Fonts(float density) {
    this.density = density;
    FreeTypeFontGenerator monoRegular = generator(MONO_REGULAR);
    mono10 = bake(monoRegular, 10);
    mono11 = bake(monoRegular, 11);
    mono12 = bake(monoRegular, 12);
    mono13 = bake(monoRegular, 13);
    monoRegular.dispose();

    FreeTypeFontGenerator monoSemibold = generator(MONO_SEMIBOLD);
    mono14 = bake(monoSemibold, 14);
    monoSemibold.dispose();

    FreeTypeFontGenerator sansBold = generator(SANS_BOLD);
    sansDisplay = bake(sansBold, 30);
    sansWordmark = bake(sansBold, 14);
    sansBold.dispose();

    FreeTypeFontGenerator sansSemibold = generator(SANS_SEMIBOLD);
    sansTitle = bake(sansSemibold, 18);
    sansButton = bake(sansSemibold, 13);
    sansSemibold.dispose();
  }

  public float density() {
    return density;
  }

  public float width(BitmapFont font, CharSequence text) {
    layout.setText(font, text);
    return layout.width;
  }

  public GlyphLayout layout(BitmapFont font, CharSequence text) {
    layout.setText(font, text);
    return layout;
  }

  private static FreeTypeFontGenerator generator(String path) {
    return new FreeTypeFontGenerator(Gdx.files.classpath(path));
  }

  private static final String EXTRA_CHARS = "←↑↓→‖×°—·•≥…";

  private BitmapFont bake(FreeTypeFontGenerator gen, int logicalPx) {
    FreeTypeFontParameter p = new FreeTypeFontParameter();
    p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + EXTRA_CHARS;
    p.size = Math.max(1, Math.round(logicalPx * density));
    p.minFilter = TextureFilter.Linear;
    p.magFilter = TextureFilter.Linear;
    p.kerning = true;
    p.genMipMaps = false;
    BitmapFont font = gen.generateFont(p);
    font.getData().setScale(1f / density);
    font.setUseIntegerPositions(false);
    return font;
  }

  public void dispose() {
    mono10.dispose();
    mono11.dispose();
    mono12.dispose();
    mono13.dispose();
    mono14.dispose();
    sansDisplay.dispose();
    sansTitle.dispose();
    sansWordmark.dispose();
    sansButton.dispose();
  }
}
