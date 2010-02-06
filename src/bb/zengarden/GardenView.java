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

import java.util.ArrayList;
import java.util.Random;

import bb.zengarden.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * SnakeView: implementation of a simple game of Snake
 * 
 * 
 */
public class GardenView extends TileView {

    private static final String TAG = "GardenView";

    /**
     * Current mode of application: READY to run, RUNNING, or you have already
     * lost. static final ints are used instead of an enum for performance
     * reasons.
     */
    private int mMode = READY;
    public static final int PAUSE = 0;
    public static final int READY = 1;
    public static final int RUNNING = 2;

    /**
     * Labels for the drawables that will be loaded into the TileView class
     */
    private static final int TILE_ROCK          = 1;
    private static final int TILE_TOP           = 2;
    private static final int TILE_BOTTOM        = 3;
    private static final int TILE_LEFT          = 4;
    private static final int TILE_RIGHT         = 5;
    private static final int TILE_TOP_LEFT      = 6;
    private static final int TILE_TOP_RIGHT     = 7;
    private static final int TILE_BOTTOM_LEFT   = 8;
    private static final int TILE_BOTTOM_RIGHT  = 9;    
    
    /* rock features */
    private Drawable[] features = new Drawable[4];
    /* current ones on the screen */
    private ArrayList<FeaturePiece> curr_features = new ArrayList<FeaturePiece>();
    //private int threshold = 10; /* we will set this based on tile size */
    private int ready_feature;

    /**
     * mStatusText: text shows to the user in some run states
     */
    private TextView mStatusText;


    /**
     * Everyone needs a little randomness in their life
     */
    private static final Random RNG = new Random();

    /**
     * Create a simple handler that we can use to cause animation to happen.  We
     * set ourselves as a target and we can use the sleep()
     * function to cause an update/invalidate to occur at a later date.
     */
    private RefreshHandler mRedrawHandler = new RefreshHandler();

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            GardenView.this.update();
            GardenView.this.invalidate();
        }

        public void sleep(long delayMillis) {
        	this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
        
        public void update() {
            this.removeMessages(0);
            sendMessage(obtainMessage(0));
        }        
    };


    public GardenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGardenView();
   }

    public GardenView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    	initGardenView();
    }
    
    private void initGardenView() {
        setFocusable(true);
        Resources r = this.getContext().getResources();
        resetTiles(14);
        
        loadTile(TILE_ROCK,         r.getDrawable(R.drawable.rock_tile));
        loadTile(TILE_TOP,          r.getDrawable(R.drawable.top));
        loadTile(TILE_BOTTOM,       r.getDrawable(R.drawable.bottom));
        loadTile(TILE_LEFT,         r.getDrawable(R.drawable.left));
        loadTile(TILE_RIGHT,        r.getDrawable(R.drawable.right));
        loadTile(TILE_TOP_LEFT,     r.getDrawable(R.drawable.top_left));
        loadTile(TILE_TOP_RIGHT,    r.getDrawable(R.drawable.top_right));
        loadTile(TILE_BOTTOM_LEFT,  r.getDrawable(R.drawable.bottom_left));
        loadTile(TILE_BOTTOM_RIGHT, r.getDrawable(R.drawable.bottom_right));
        
        //rr = r.getDrawable(R.drawable.rock4);
        
        features[0] = r.getDrawable(R.drawable.rock1);
        features[1] = r.getDrawable(R.drawable.rock2);
        features[2] = r.getDrawable(R.drawable.rock3);
        features[3] = r.getDrawable(R.drawable.rock4);
        
        //threshold = mTileSize/2;
        ready_feature = 0;
              
    }
    

    private void initNewGame() {
        curr_features.clear();
    }


    /**
     * Given a ArrayList of coordinates, we need to flatten them into an array of
     * ints before we can stuff them into a map for flattening and storage.
     * 
     * @param cvec : a ArrayList of Coordinate objects
     * @return : a simple array containing the x/y values of the coordinates
     * as [x1,y1,x2,y2,x3,y3...]
     */
    private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
        int count = cvec.size();
        int[] rawArray = new int[count * 2];
        for (int index = 0; index < count; index++) {
            Coordinate c = cvec.get(index);
            rawArray[2 * index] = c.x;
            rawArray[2 * index + 1] = c.y;
        }
        return rawArray;
    }

    /**
     * Save game state so that the user does not lose anything
     * if the game process is killed while we are in the 
     * background.
     * 
     * @return a Bundle with this view's state
     */
    public Bundle saveState() {
        Bundle map = new Bundle();

        //map.putIntArray("mAppleList", coordArrayListToArray(curr_features));

        return map;
    }

    /**
     * Given a flattened array of ordinate pairs, we reconstitute them into a
     * ArrayList of Coordinate objects
     * 
     * @param rawArray : [x1,y1,x2,y2,...]
     * @return a ArrayList of Coordinates
     */
    private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
        ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 2) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            coordArrayList.add(c);
        }
        return coordArrayList;
    }

    /**
     * Restore game state if our process is being relaunched
     * 
     * @param icicle a Bundle containing the game state
     */
    public void restoreState(Bundle icicle) {
        setMode(PAUSE);

        //curr_features = coordArrayToArrayList(icicle.getIntArray("mAppleList"));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
        Log.d(TAG, " event x:" + event.getX() + ", event y:" + event.getY());
        // translate into tile coords
        int x = (int) (event.getX()  - mXOffset )/ mTileSize;
        int y = (int) (event.getY()  - mYOffset )/ mTileSize;
        Log.d(TAG, " x:" + x + ", y:" + y);
        
        // replace feature or find a feature to remove;
        boolean replaced = false;
        
        ArrayList<FeaturePiece> removeList = new ArrayList<FeaturePiece>();
        for (FeaturePiece piece : curr_features) {
            if (piece.equals(new Coordinate(x, y))) {
                piece.setFeature(piece.getFeature() + 1);
                replaced = true;
            }
            
            // flag for removal
            if (piece.getFeature() == features.length)
                removeList.add(piece);
        }
        
        // removed the flags
        for (FeaturePiece piece : removeList) {
            curr_features.remove(piece);
        }
        
        
        if (!replaced)
            curr_features.add(new FeaturePiece(0, x, y));
        
    	update();
    	return super.onTouchEvent(event);
    }
    
    /*
     * handles key events in the game. Update the direction our snake is traveling
     * based on the DPAD. Ignore events that would cause the snake to immediately
     * turn back on itself.
     * 
     * (non-Javadoc)
     * 
     * @see android.view.View#onKeyDown(int, android.os.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (mMode == READY) {
                /*
                 * At the beginning of the game, or the end of a previous one,
                 * we should start a new game.
                 */
                initNewGame();
                setMode(RUNNING);
                update();
                return (true);
            }

            if (mMode == PAUSE) {
                /*
                 * If the game is merely paused, we should just continue where
                 * we left off.
                 */
                setMode(RUNNING);
                update();
                return (true);
            }

            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return (true);
        }

        return super.onKeyDown(keyCode, msg);
    }

    /**
     * Sets the TextView that will be used to give information (such as "Game
     * Over" to the user.
     * 
     * @param newView
     */
    public void setTextView(TextView newView) {
        mStatusText = newView;
    }

    /**
     * Updates the current mode of the application (RUNNING or PAUSED or the like)
     * as well as sets the visibility of textview for notification
     * 
     * @param newMode
     */
    public void setMode(int newMode) {
        int oldMode = mMode;
        mMode = newMode;

        if (newMode == RUNNING & oldMode != RUNNING) {
            mStatusText.setVisibility(View.INVISIBLE);
            update();
            return;
        }

        Resources res = getContext().getResources();
        CharSequence str = "";
        if (newMode == PAUSE) {
            str = res.getText(R.string.mode_pause);
        }
        if (newMode == READY) {
            str = res.getText(R.string.mode_ready);
        }
        
        mStatusText.setText(str);
        mStatusText.setVisibility(View.VISIBLE);
    }

    /**
     * Handles the basic update loop, checking to see if we are in the running
     * state, determining if a move should be made, updating the snake's location.
     */
    public void update() {
        if (mMode == RUNNING) {
            clearTiles();
            updateBackground();
            mRedrawHandler.update();
        }

    }

    
    @Override
    public void onDraw(Canvas canvas) {
    	super.onDraw(canvas);
    	for (FeaturePiece c : curr_features) {
    		int x = mXOffset + c.x * mTileSize;
    		int y = mYOffset + c.y * mTileSize;
    		//Drawable d = features[c.getFeature()];
    		features[c.getFeature()].setBounds(x, y, x + mTileSize, y + mTileSize);
    		features[c.getFeature()].draw(canvas);
    	}
    }
    
    
    /**
     * Draws some walls.
     * 
     */
    private void updateBackground() {

    	for (int x = 0; x < mXTileCount; x++) {
    		if (x==0) System.out.println("~~~~~~~~~~~~~~ " + mTileSize);
    		for (int y = 0; y < mYTileCount; y++) {
    			if (	 x == 0 && 					y == 0) setTile(TILE_TOP_LEFT, x, y);
    			else if (x == mXTileCount - 1 && 	y == 0) setTile(TILE_TOP_RIGHT, x, y);
    			else if (x == 0 && 					y == mYTileCount -1) setTile(TILE_BOTTOM_LEFT, x, y);
    			else if (x == mXTileCount - 1 && 	y == mYTileCount - 1) setTile(TILE_BOTTOM_RIGHT, x, y);
    			else if (y == 0) setTile(TILE_TOP, x, y);
    			else if (x == 0) setTile(TILE_LEFT, x, y);
    			else if (y == mYTileCount - 1) setTile(TILE_BOTTOM, x, y);
    			else if (x == mXTileCount - 1) setTile(TILE_RIGHT, x, y);
    			else setTile(TILE_ROCK, x, y);
    		}
    	}

    }
    
    
    private class FeaturePiece extends Coordinate {
        private int feature;
        public FeaturePiece(int feature, int newX, int newY) {
            super(newX, newY);
            this.feature = feature;
        }
        public int getFeature() {
            return feature;
        }
        public void setFeature(int feature) {
            this.feature = feature;
        }
    }
    
    

    /**
     * Simple class containing two integer values and a comparison function.
     * There's probably something I should use instead, but this was quick and
     * easy to build.
     * 
     */
    private class Coordinate {
        public int x;
        public int y;

        public Coordinate(int newX, int newY) {
            x = newX;
            y = newY;
        }

        public boolean equals(Coordinate other) {
            if (x == other.x && y == other.y) {
                return true;
            }
            return false;
        }

        public boolean isNear(int x, int y, int threshold) {
            return distance(x, y) <= threshold;
        }
        
        public boolean isNear(Coordinate other, int threshold) {
            return isNear(other.x, other.y, threshold);
        }
        
        public int distance(Coordinate other) {
            return distance(other.x, other.y);
        }
        
        public int distance(int x2, int y2) {
            return (int) Math.sqrt(
                    Math.pow((x2 - x), 2) + 
                    Math.pow((y2 - y), 2)
            );
        }
        
        @Override
        public String toString() {
            return "Coordinate: [" + x + "," + y + "]";
        }
    }
    
}
