/* 
 * Copyright (C) 2010 Brian Ballantine 
 * Apache License, Version 2.0 
 * 
 * Much of this code was taken from the Android Snake project:
 * 
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bb.zengarden;

import bb.zengarden.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;


public class Garden extends Activity {

	private GardenView garden_view;
    private static String KEY = "zengarden-view";

    /**
     * Called when Activity is first created. Turns off the title bar, sets up
     * the content views, and fires up the SnakeView.
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No Title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.garden_layout);

        garden_view = (GardenView) findViewById(R.id.garden);
        garden_view.setTextView((TextView) findViewById(R.id.text));

        if (savedInstanceState == null) {
            // We were just launched -- set up a new game
            garden_view.setMode(GardenView.READY);
        } else {
            // We are being restored
            Bundle map = savedInstanceState.getBundle(KEY);
            if (map != null) {
                garden_view.restoreState(map);
            } else {
                garden_view.setMode(GardenView.PAUSE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the game along with the activity
        garden_view.setMode(GardenView.PAUSE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Store the game state
        outState.putBundle(KEY, garden_view.saveState());
    }

}
