package com.signalcollect.javaapi.examples;

import java.awt.*;
import java.util.Random;
import javax.swing.*;

/**
 * Utility do display data with a limited amount of values in a grid.
 * 
 * @author Daniel Strebel
 *
 */
public class PixelMap extends JComponent {

	private static final long serialVersionUID = 1L;
	private int width = 500;
	private int height = 500;
	private Random rand = new Random();
	private byte[] imageValues;
	JFrame window;

	public PixelMap() {
		window = new JFrame();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setBounds(30, 30, width, height);
		PixelMap image = this;
		window.getContentPane().add(image);
	}

	/**
	 * Sets the data for display and updates the image.
	 * 
	 * @param data the data to be displayed.
	 */
	public void setData(byte[] data) {
		imageValues = data;
		this.repaint();
		window.setVisible(true);
	}

	/**
	 * Displays an image if the data is available.
	 * 
	 * @note the data will be displayed as a suqared image.
	 */
	public void paint(Graphics g) {
		if (imageValues != null) {
			
			// determine the scaling
			int widthOfPixel = width
					/ (int) Math.floor(Math.sqrt(imageValues.length));
			int heightOfPixel = widthOfPixel;
			
			//Print the pixels
			for (int i = 0; i < imageValues.length; i++) {
				if (imageValues[i] == 0) {
					g.setColor(Color.blue);
				} else {
					g.setColor(Color.gray);
				}
				g.fillRect((i % (width / widthOfPixel)) * widthOfPixel,
						(i / (height / heightOfPixel)) * heightOfPixel,
						widthOfPixel, heightOfPixel);
			}
		}
	}
}
