package com.group5.paul_esys.ui;

import com.formdev.flatlaf.ui.FlatDropShadowBorder;
import com.formdev.flatlaf.ui.FlatRoundBorder;
import java.awt.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

public class RoundShadowBorder extends CompoundBorder {

	public static Border getBorderWithShadow() {
		return new FlatDropShadowBorder(
		    Color.BLACK,
		    8,
		    0.3f
		);
	}

	public static Border getFlatRoundBorder() {
		return new FlatRoundBorder();
	}

	public RoundShadowBorder() {
		super(
		    new FlatDropShadowBorder(Color.BLACK, 8, 0.3f),
		    new FlatRoundBorder()
		);
	}
}
