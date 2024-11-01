package com.conickel.tunestats;


import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Page {
	public String title;
	public String description;
	public String imageURL;

	public Page(String title, String description, String imageURL) {
		this.title = title;
		this.description = description;
		this.imageURL = imageURL;
	}
}
