// Draws the launcher icon.
//
//   java Tools/gen-icon.java showcase/src/main/res
//
// Run by hand, never in CI. The mark is the one thing this library does that no
// other text view does: a digit half way out of its slot with the next one half
// way in, clipped to the slot and soft at the edges, which is exactly the
// arrangement described in MorphTiming and drawn by TextMorphCanvas.
//
// Java2D rather than the library itself, for the same reason the iOS twin uses
// CoreGraphics rather than its own package: the icon should be regenerable
// without building anything, and on any machine rather than only on a Mac.
// Tools/gen-icon.swift over there and this file draw the same picture, and the
// two are meant to be read side by side.

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class GenIcon {
    /** Near black, and an off-white paper, matching the iOS twin exactly. */
    private static final Color INK = new Color(18, 18, 23);
    private static final Color PAPER = new Color(250, 250, 247);

    /** The band the slot's edges are softened over, in ems. MorphTiming names it. */
    private static final double SLOT_FADE = 0.15;

    /** Legacy launcher icons: the whole square is the mark. */
    private static final String[] DENSITIES = {
        "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi",
    };
    private static final int[] LEGACY = {48, 72, 96, 144, 192};

    /**
     * Adaptive foregrounds are 108dp with only the middle 72dp guaranteed
     * visible, so the mark is drawn at two thirds of the square and the rest is
     * the margin a launcher is free to mask away.
     */
    private static final int[] ADAPTIVE = {162, 216, 288, 432, 576};

    public static void main(String[] args) throws IOException {
        File res = new File(args.length > 0 ? args[0] : "showcase/src/main/res");

        for (int i = 0; i < DENSITIES.length; i++) {
            File dir = new File(res, "mipmap-" + DENSITIES[i]);
            dir.mkdirs();
            write(mark(LEGACY[i], 1.0, true), new File(dir, "ic_launcher.png"));
            write(mark(ADAPTIVE[i], 2.0 / 3.0, false), new File(dir, "ic_launcher_foreground.png"));
        }
        System.out.println("wrote " + DENSITIES.length * 2 + " files under " + res);
    }

    /**
     * The mark: a four on its way out downwards at low opacity, a five arriving
     * from above, both clipped to one line box and soft at its edges.
     *
     * @param side the square's side, in pixels
     * @param inset how much of the square the mark occupies, for the adaptive
     *              foreground's safe zone
     * @param opaque whether to fill the paper, which an adaptive foreground must
     *               not do because the background layer is a separate one
     */
    private static BufferedImage mark(int side, double inset, boolean opaque) {
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (opaque) {
            g.setColor(PAPER);
            g.fillRect(0, 0, side, side);
        }

        double size = side * 0.62 * inset;
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.round(size));
        g.setFont(font);
        double ascent = g.getFontMetrics().getAscent();
        double descent = g.getFontMetrics().getDescent();

        // The slot: one line box, centred, with the horizontal axis left open
        // the way the library leaves it open so glyph overhang is not shaved.
        double slotHeight = ascent + descent;
        double slotTop = (side - slotHeight) / 2;
        double baseline = slotTop + ascent;
        double centre = (side - g.getFontMetrics().stringWidth("5")) / 2.0;

        // The digits and the mask go in one layer, so the mask cannot eat the
        // paper underneath them.
        BufferedImage layer = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D lg = layer.createGraphics();
        lg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        lg.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        lg.setFont(font);
        lg.clip(new Rectangle2D.Double(0, slotTop, side, slotHeight));

        // One third of a line box, which is about where a roll reads most
        // clearly.
        double slide = slotHeight / 3;
        lg.setColor(new Color(INK.getRed(), INK.getGreen(), INK.getBlue(), 71));
        lg.drawString("4", (float) centre, (float) (baseline + slide));
        lg.setColor(INK);
        lg.drawString("5", (float) centre, (float) (baseline - slide * 0.15));

        double band = size * SLOT_FADE;
        lg.setComposite(AlphaComposite.DstIn);
        lg.setPaint(new LinearGradientPaint(
            new Point2D.Double(side / 2.0, slotTop),
            new Point2D.Double(side / 2.0, slotTop + slotHeight),
            new float[] {
                0f,
                (float) (band / slotHeight),
                (float) (1 - band / slotHeight),
                1f,
            },
            new Color[] {
                new Color(0, 0, 0, 0),
                new Color(0, 0, 0, 255),
                new Color(0, 0, 0, 255),
                new Color(0, 0, 0, 0),
            }));
        lg.fill(new Rectangle2D.Double(0, slotTop, side, slotHeight));
        lg.dispose();

        g.drawImage(layer, 0, 0, null);
        g.dispose();
        return image;
    }

    private static void write(BufferedImage image, File file) throws IOException {
        ImageIO.write(image, "png", file);
    }
}
