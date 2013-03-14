package nl.inl.impact.ner.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class Colors {

	private static Color[] basicColors = new Color[] { new Color(0, 102, 104), /*
		 * B-
		 * LOC
		 */
		new Color(51, 102, 51), new Color(153, 102, 0), new Color(204, 102, 0),
		new Color(204, 102, 0), /* B-ORG */
		new Color(204, 0, 102), /* B-PER */
		new Color(153, 0, 0), new Color(153, 0, 204),
		new Color(255, 102, 0), new Color(255, 102, 153),
		new Color(204, 152, 255), new Color(102, 102, 255)

	};
	
	public static Color[] getNColors(int n) {
		Color[] colors = new Color[n];
		if (n <= basicColors.length) {
			System.arraycopy(basicColors, 0, colors, 0, n);
		} else {
			int s = 255 / (int) Math.ceil(Math.pow(n, (1.0 / 3.0)));
			int index = 0;
			OUTER: for (int i = 0; i < 256; i += s) {
				for (int j = 0; j < 256; j += s) {
					for (int k = 0; k < 256; k += s) {
						colors[index++] = new Color(i, j, k);
						if (index == n) {
							break OUTER;
						}
					}
				}
			}
		}
		return colors;
	}

	
	public static String colorToHTML(Color color) {
		String r = Integer.toHexString(color.getRed());
		if (r.length() == 0) {
			r = "00";
		} else if (r.length() == 1) {
			r = "0" + r;
		} else if (r.length() > 2) {
			System.err.println("invalid hex color for red" + r);
		}

		String g = Integer.toHexString(color.getGreen());
		if (g.length() == 0) {
			g = "00";
		} else if (g.length() == 1) {
			g = "0" + g;
		} else if (g.length() > 2) {
			System.err.println("invalid hex color for green" + g);
		}

		String b = Integer.toHexString(color.getBlue());
		if (b.length() == 0) {
			b = "00";
		} else if (b.length() == 1) {
			b = "0" + b;
		} else if (b.length() > 2) {
			System.err.println("invalid hex color for blue" + b);
		}

		return "#" + r + g + b;
	}

	public static class ColorIcon implements Icon {
		Color color;

		public ColorIcon(Color c) {
			color = c;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(color);
			g.fillRect(x, y, getIconWidth(), getIconHeight());
		}

		public int getIconWidth() {
			return 10;
		}

		public int getIconHeight() {
			return 10;
		}
	}

	
}
