
package ros.android.exhibit;

import org.ros.exception.RemoteException;
import org.ros.exception.RosException;
import ros.android.activity.AppManager;
import ros.android.activity.RosAppActivity;
import android.os.Bundle;
import org.ros.node.Node;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;
import org.ros.node.service.ServiceClient;
import org.ros.node.topic.Publisher;
import org.ros.service.app_manager.StartApp;
import org.ros.node.service.ServiceResponseListener;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.LinearLayout;
import org.ros.service.std_srvs.Empty;
import org.ros.message.trajectory_msgs.JointTrajectory;
import org.ros.message.trajectory_msgs.JointTrajectoryPoint;
import java.util.ArrayList;
import org.ros.message.Duration;
import android.content.Intent;
import android.content.Context;
import java.lang.Class;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.content.res.Resources;
import android.widget.TabHost;
import android.content.DialogInterface;
import android.app.TabActivity; 
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import java.lang.String;
import java.util.Timer;
import java.util.TimerTask;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ImageButton;

import org.ros.message.program_queue.Program;
/*import org.ros.message.program_queue.ProgramInfo;
import org.ros.message.program_queue.Output;
import org.ros.service.program_queue.GetProgram;
import org.ros.service.program_queue.GetMyPrograms;
import org.ros.service.program_queue.GetPrograms;
import org.ros.service.program_queue.Login;
import org.ros.service.program_queue.Logout;
import org.ros.service.program_queue.ClearQueue;
import org.ros.service.program_queue.CreateUser;
import org.ros.service.program_queue.CreateProgram;
import org.ros.service.program_queue.DequeueProgram;
import org.ros.service.program_queue.GetOutput;
import org.ros.service.program_queue.GetQueue;
import org.ros.service.program_queue.QueueProgram;
import org.ros.service.program_queue.RunProgram;
import org.ros.service.program_queue.UpdateProgram;
*/

import org.ros.message.turtlebot_exhibit.Relationship; 

//import com.blahti.example.drag3.R;
import ros.android.exhibit.R;

import android.app.Activity;
import android.os.Bundle;

import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.ArrayList;
import java.lang.Integer;
import java.util.List;

/**
 * This activity presents a screen with a grid on which images can be added and moved around.
 * It also defines areas on the screen where the dragged views can be dropped. Feedback is
 * provided to the user as the objects are dragged over these drop zones.
 *
 * <p> Like the ExhibitActivity in the previous version of the DragView example application, the
 * code here is derived from the Android Launcher code.
 * 
 * <p> The original Launcher code required a long click (press) to initiate a drag-drop sequence.
 * If you want to see that behavior, set the variable mLongClickStartsDrag to true.
 * It is set to false below, which means that any touch event starts a drag-drop.
 * 
 */

public class ExhibitActivity extends RosAppActivity 
    implements View.OnLongClickListener, View.OnClickListener,
               View.OnTouchListener //  , AdapterView.OnItemClickListener
{

	

/**
 */
// Constants

private static final int HIDE_TRASHCAN_MENU_ID = Menu.FIRST;
private static final int SHOW_TRASHCAN_MENU_ID = Menu.FIRST + 1;
private static final int ADD_OBJECT_MENU_ID = Menu.FIRST + 2;
private static final int CHANGE_TOUCH_MODE_MENU_ID = Menu.FIRST + 3;

private static final int CW = 1;
private static final int CCW = 2;
private static final int FW = 3;
private static final int BW = 4;
private static final int COW = 5;

private static final int PROX_CLOSE = 6;
private static final int PROX_FAR = 7;

/**
 */
// Variables

private DragController mDragController;   // Object that handles a drag-drop sequence. It intersacts with DragSource and DropTarget objects.
private DragLayer mDragLayer;             // The ViewGroup within which an object can be dragged.
private DeleteZone mDeleteZone;           // A drop target that is used to remove objects from the screen.
private int mImageCount = 0;              // The number of images that have been added to screen.
private ImageCell mLastNewCell = null;    // The last ImageCell added to the screen when Add Image is clicked.
private boolean mLongClickStartsDrag = false;   // If true, it takes a long click to start the drag operation.
                                                // Otherwise, any touch event starts a drag.

public static final boolean Debugging = false;   // Use this to see extra toast messages.

  private String topic = "/stim_react";
  private Relationship msg;
  //private Thread pubThread;
  private Publisher<Relationship> relPub;
  //private boolean sendMessages = true;
  //private boolean nullMessage = true;



/**
 */
// Methods

/**
 * Add a new image so the user can move it around. It shows up in the image_source_frame
 * part of the screen.
 * 
 * @param resourceId int - the resource id of the image to be added
 */    

public void addNewImageToScreen (int resourceId)
{
    if (mLastNewCell != null) mLastNewCell.setVisibility (View.GONE);

    FrameLayout imageHolder = (FrameLayout) findViewById (R.id.image_source_frame);
    if (imageHolder != null) {
       FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams (LayoutParams.FILL_PARENT, 
                                                                   LayoutParams.FILL_PARENT, 
                                                                   Gravity.CENTER);
       ImageCell newView = new ImageCell (this);
       newView.setImageResource (resourceId);
       imageHolder.addView (newView, lp);
       newView.mEmpty = false;
       newView.mCellNumber = -1;
       mLastNewCell = newView;
       mImageCount++;

       // Have this activity listen to touch and click events for the view.
       newView.setOnClickListener(this);
       newView.setOnLongClickListener(this);
       newView.setOnTouchListener (this);

      // saveSend();

    }
}

/**
 * Add one of the images to the screen so the user has a new image to move around. 
 * See addImageToScreen.
 *
 */    
public void saveSend(View v)
{

//get array from DragController
//process it into a message
//publish to topic 
//create publisher in onNodeCreate and destroy in onNodeDestroy

  ArrayList<Integer> puzzleArrangement = mDragController.getImagePositions();
  for (int i = 0; i < puzzleArrangement.size(); i++)
  {
    Log.i("ExhibitActivity", "this place is: " + puzzleArrangement.get(i));
  }

  //check to make sure there aren't hanging ifs, etc

  msg = new Relationship();
  for (int i = 0; i <= 10; i+=5) 
  {
    if (puzzleArrangement.get(i) != -1)
    {
      int gap = 0;
      ArrayList<Integer> reactions = new ArrayList<Integer>();
      for (int j = i; j <= i+4; j++)
      {
        if (puzzleArrangement.get(j) !=-1)
        {
          if (gap > 0)
          { 
            toast ("Uh oh! There are gaps in your puzzle!");
            return;
          }
          else
          {
          reactions.add(puzzleArrangement.get(j));
          }
        }
        else
        {
          gap++;
        }
      }
      if (puzzleArrangement.get(i) == PROX_CLOSE)
      {
        msg.prox2 = convertIntegers(reactions);
      }
      else if (puzzleArrangement.get(i) == PROX_FAR)
      {
        msg.prox4 = convertIntegers(reactions);
      }
      else
      {
        toast ("You can't start with a 'then' piece!");
        return;
      }
    }
  }  

  relPub.publish(msg);
  Log.i("ExhibitActivity", "Publish");
}

public static int[] convertIntegers(List<Integer> integers)
{
    int[] ret = new int[integers.size()];
    for (int i=0; i < ret.length; i++)
    {
        ret[i] = integers.get(i).intValue();
    }
    return ret;
}

/**
 * Handle a click on a view.
 *
 */    

public void onClick(View v) 
{
    if (mLongClickStartsDrag) {
       // Tell the user that it takes a long click to start dragging.
       toast ("Press and hold to drag an image.");
    }
}

/**
 * Handle a click of the Add Image button
 *
 */    

public void onClickAddImage (View v) 
{
        if (v.getId() == R.id.button_add_then_cw)
        {
      	  addNewImageToScreen (R.drawable.puzzle_then_cw);
          mDragController.setRecentButton(1);

    	}
	if (v.getId() == R.id.button_add_then_ccw)
        {
	addNewImageToScreen (R.drawable.puzzle_then_ccw);
        mDragController.setRecentButton(2);
	}

	if (v.getId() == R.id.button_add_then_forwards)
	{
          addNewImageToScreen (R.drawable.puzzle_then_forwards);
	          mDragController.setRecentButton(3);
        }
	if (v.getId() == R.id.button_add_then_backwards)
	{
          addNewImageToScreen (R.drawable.puzzle_then_backwards);
	          mDragController.setRecentButton(4);
        }
	if (v.getId() == R.id.button_add_then_cow)
	{
          addNewImageToScreen (R.drawable.puzzle_then_cow);
	          mDragController.setRecentButton(5);
        }
	if (v.getId() == R.id.button_add_if_2ft)
	{
          addNewImageToScreen (R.drawable.puzzle_if_2ft);
	  mDragController.setRecentButton(6);
        Button button = (Button) findViewById(R.id.button_add_if_2ft);
        button.setBackgroundResource(R.drawable.puzzle_if_2ft_disable);
        button.setEnabled(false);
        }
	if (v.getId() == R.id.button_add_if_8ft)
	{
        	addNewImageToScreen (R.drawable.puzzle_if_8ft);
	          mDragController.setRecentButton(7);
        Button button = (Button) findViewById(R.id.button_add_if_8ft);
        button.setBackgroundResource(R.drawable.puzzle_if_8ft_disable);
        button.setEnabled(false);
        }
	
}

/**
 * onCreate - called when the activity is first created.
 * 
 * Creates a drag controller and sets up three views so click and long click on the views are sent to this activity.
 * The onLongClick method starts a drag sequence.
 *
 */

 public void onCreate(Bundle savedInstanceState) 
{

    setDefaultAppName("turtlebot_exhibit/android_turtlebot_exhibit");
    setDashboardResource(R.id.top_bar);
    setMainWindowResource(R.layout.main);

    super.onCreate(savedInstanceState);


    setContentView(R.layout.main);

    GridView gridView = (GridView) findViewById(R.id.image_grid_view);

    if (gridView == null) toast ("Unable to find GridView");
    else {
         gridView.setAdapter (new ImageCellAdapter(this));
         // gridView.setOnItemClickListener (this);
    }

    mDragController = new DragController(this);
    mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
    mDragLayer.setDragController (mDragController);
    mDragLayer.setGridView (gridView);

    mDragController.setDragListener (mDragLayer);
    // mDragController.addDropTarget (mDragLayer);

    mDeleteZone = (DeleteZone) findViewById (R.id.delete_zone_view);

    // Give the user a little guidance.
    Toast.makeText (getApplicationContext(), 
                    getResources ().getString (R.string.instructions),
                    Toast.LENGTH_LONG).show ();

}


  @Override
  protected void onNodeDestroy(Node node) {
    /*if (cameraView != null) {
      cameraView.stop();
      cameraView = null;
    }
    if (joystickView != null) {
      joystickView.stop();
      joystickView = null;
    }*/

    if (relPub != null) {
      relPub.shutdown();
      relPub = null;
    }
    super.onNodeDestroy(node);

  }


  @Override
  protected void onNodeCreate(Node node) {
    Log.i("ExhibitActivity", "startAppFuture");
    super.onNodeCreate(node);
      //NameResolver appNamespace = getAppNamespace(node);
      /*cameraView.start(node, appNamespace.resolve(cameraTopic).toString());
      cameraView.post(new Runnable() {
        @Override
        public void run() {
          cameraView.setSelected(true);
        }});
      joystickView.start(node);
      */

    relPub = node.newPublisher(topic, "turtlebot_exhibit/Relationship");
    //createPublisherThread(twistPub, touchCmdMessage, 10);

  }


/**
 * Build a menu for the activity.
 *
 */    

public boolean onCreateOptionsMenu (Menu menu) 
{
    super.onCreateOptionsMenu(menu);
    
    menu.add(0, HIDE_TRASHCAN_MENU_ID, 0, "Hide Trashcan").setShortcut('1', 'c');
    menu.add(0, SHOW_TRASHCAN_MENU_ID, 0, "Show Trashcan").setShortcut('2', 'c');
    menu.add(0, ADD_OBJECT_MENU_ID, 0, "Add View").setShortcut('9', 'z');
    menu.add (0, CHANGE_TOUCH_MODE_MENU_ID, 0, "Change Touch Mode");


    return true;
}

/**
 * Handle a click of an item in the grid of cells.
 * 
 */

public void onItemClick(AdapterView<?> parent, View v, int position, long id) 
{
    ImageCell i = (ImageCell) v;
    trace ("onItemClick in view: " + i.mCellNumber);
}

/**
 * Handle a long click.
 * If mLongClick only is true, this will be the only way to start a drag operation.
 *
 * @param v View
 * @return boolean - true indicates that the event was handled
 */    

public boolean onLongClick(View v) 
{
    if (mLongClickStartsDrag) {
       
        //trace ("onLongClick in view: " + v + " touchMode: " + v.isInTouchMode ());

        // Make sure the drag was started by a long press as opposed to a long click.
        // (Note: I got this from the Workspace object in the Android Launcher code. 
        //  I think it is here to ensure that the device is still in touch mode as we start the drag operation.)
        if (!v.isInTouchMode()) {
           toast ("isInTouchMode returned false. Try touching the view again.");
           return false;
        }
        return startDrag (v);
        
    }

    // If we get here, return false to indicate that we have not taken care of the event.
    return false;
}

/**
 * This is the starting point for a drag operation if mLongClickStartsDrag is false.
 * It looks for the down event that gets generated when a user touches the screen.
 * Only that initiates the drag-drop sequence.
 *
 */    

public boolean onTouch(View v, MotionEvent ev) 
{
    // If we are configured to start only on a long click, we are not going to handle any events here.
    if (mLongClickStartsDrag) return false;

    boolean handledHere = false;

    final int action = ev.getAction();

    // In the situation where a long click is not needed to initiate a drag, simply start on the down event.
    if (action == MotionEvent.ACTION_DOWN) {
       handledHere = startDrag (v);
    }
    if(action == MotionEvent.ACTION_UP){
      ArrayList<Integer> puzzleArrangement = mDragController.getImagePositions();
      if (!puzzleArrangement.contains(6))
      {
        Button button = (Button) findViewById(R.id.button_add_if_2ft);
        button.setBackgroundResource(R.drawable.puzzle_if_2ft);
        button.setEnabled(true);

      }	
      if (!puzzleArrangement.contains(7))
      {
        Button button = (Button) findViewById(R.id.button_add_if_8ft);
        button.setBackgroundResource(R.drawable.puzzle_if_8ft);
        button.setEnabled(true);

      }

    }
    
    return handledHere;
}   

/**
 * Start dragging a view.
 *
 */    

public boolean startDrag(View v)
{
    DragSource dragSource = (DragSource) v;

    // We are starting a drag. Let the DragController handle it.
    mDragController.startDrag (v, dragSource, dragSource, DragController.DRAG_ACTION_MOVE);

    return true;
}

/**
 * Show a string on the screen via Toast.
 * 
 * @param msg String
 * @return void
 */

public void toast(String msg)
{
    Toast.makeText (getApplicationContext(), msg, Toast.LENGTH_SHORT).show ();
} // end toast

/**
 * Send a message to the debug log. Also display it using Toast if Debugging is true.
 */

public void trace(String msg) 
{
    Log.d ("ExhibitActivity", msg);
    if (!Debugging) return;
    toast (msg);
}

} // end class
