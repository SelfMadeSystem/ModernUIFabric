/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.selfmadesystem.modernuifabric.testfabric;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.RelativeLayout;

import javax.annotation.Nonnull;

public class TestRelativeLayout extends RelativeLayout {

    public TestRelativeLayout() {
        LayoutParams lp = new LayoutParams(40, 20);
        lp.addRule(CENTER_IN_PARENT);
        CView view = new CView();
        view.setId(1);
        view.setLayoutParams(lp);
        view.text = "First One!";
        addView(view);

        lp = new LayoutParams(60, 20);
        lp.addRule(ABOVE, 1);
        lp.addRule(CENTER_HORIZONTAL);
        view = new CView();
        view.setId(2);
        view.setLayoutParams(lp);
        view.text = "Second On The Top";
        addView(view);

        lp = new LayoutParams(LayoutParams.MATCH_PARENT, 40);
        lp.addRule(LEFT_OF, 1);
        lp.addRule(CENTER_VERTICAL);
        view = new CView();
        view.setId(3);
        view.setLayoutParams(lp);
        view.text = "3rd";
        addView(view);
    }

    private static class CView extends View {

        public String text;

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            /*canvas.resetColor();
            canvas.setTextAlign(TextAlign.CENTER);
            canvas.drawText(text, getLeft() + getWidth() / 2.0f, getTop() + 4.0f);*/
        }
    }
}
