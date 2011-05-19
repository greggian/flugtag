package com.flugtag.model;

import java.io.Serializable;
import java.text.MessageFormat;

/**
 * Represents a single line item on a Check/Receipt.
 */
public class CheckItem implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String label;
	private String priceStr;
	private double price;
	
	public CheckItem(String label, double price) {
		this.label = label;
		this.price = price;
		
		this.priceStr = MessageFormat.format("{0,number,$0.00}", price);
	}
	
	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * @return the price
	 */
	public String getPriceLabel() {
		return priceStr;
	}
	
	/**
	 * @return the price
	 */
	public double getPrice() {
		return price;
	}	
}
