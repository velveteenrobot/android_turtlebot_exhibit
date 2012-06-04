/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ros.android.scriptinterface;

import org.ros.exception.RemoteException;
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
import android.widget.EditText;
import android.widget.ImageButton;

import org.ros.message.program_queue.Program;
import org.ros.message.program_queue.ProgramInfo;
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

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author pratkanis@willowgarage.com (Tony Pratkanis)
 */
public class ScriptInterface extends RosAppActivity {
  
  private long token = 0;
  private static final int EXISTING_PROGRAM_DIALOG = 1;
  private static final int PYTHON = 1;
  private static final int PUPPETSCRIPT = 0;
  private static String DB_NAME = "favourites_db";
  private ProgressDialog progress;
  private ArrayList<ProgramInfo> my_programs;
  private EditText name_field;
  private Spinner spinner;
  private EditText program_field;
  private Program current_program = null;
  private String username;
  private int type;
  private boolean is_admin = false;
  private ArrayList<ProgramInfo> program_queue = new ArrayList();
  private ListView list;
  private ArrayList<String> queue_names = new ArrayList();
  private ArrayList<ProgramInfo> favourite_programs = new ArrayList();
  private ListView favourite_list;
  //private static final String DB_NAME = "favourite_programs";
  //private static final String TABLE_NAME = "favourite_programs";

  /** Called when the activity is first created. */

  @Override
  public void onCreate(Bundle savedInstanceState) {

    Intent startingIntent = getIntent();
    if (startingIntent.hasExtra("token")) {
      token = startingIntent.getLongExtra("token", 0);
    } else {
      finish();
    }
    if (startingIntent.hasExtra("stop")) {
      finish();
    }
    if (startingIntent.hasExtra("username")) {
      username = startingIntent.getStringExtra("username");
    }

    if (startingIntent.hasExtra("is_admin")) {
      is_admin = startingIntent.getBooleanExtra("is_admin", false);
    }

    /*db = this.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
    db.execSQL("CREATE TABLE IF NOT EXISTS " +
                        TABLE_NAME +
                        " (ProgramId INT);");
    Cursor c = sampleDB.rawQuery("SELECT ProgramId FROM " +
                        SAMPLE_TABLE_NAME, null);
    
    
    db.execSQL("DELETE * FROM " +
                        TABLE_NAME +
                       ";");   
    */
 
    setDefaultAppName("pr2_props_app/pr2_props");
    setDashboardResource(R.id.top_bar);
    setMainWindowResource(R.layout.main);
    super.onCreate(savedInstanceState);

    name_field = (EditText) findViewById(R.id.name_field);
    program_field = (EditText) findViewById(R.id.program_field);
        Button edit_btn = (Button) findViewById(R.id.edit_btn);
        edit_btn.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            //show Progress Dialog
            getMyPrograms();
            //runOnUiThread(new Runnable() {
            //  @Override
            //  public void run() {
            //    progress = ProgressDialog.show(ScriptInterface.this, "Loading", "Loading your programs...", true, true);
            //    progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            //  }
            //});

            //showDialog(EXISTING_PROGRAM_DIALOG);
          }
        });

        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.program_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

        list=(ListView)findViewById(R.id.list);
        favourite_list = (ListView)findViewById(R.id.favourites_list);
        QueueAdapter queueAdapter = new QueueAdapter();
 
        if (queueAdapter != null) {
          Log.i("ScriptInterface", "Queue Adapter not null.");
        } else {
          Log.i("ScriptInterface", "Queue Adapter null.");
        }
 
        list.setAdapter(queueAdapter);

        FavouritesAdapter favouritesAdapter = new FavouritesAdapter();
        if (favouritesAdapter != null) {
          Log.i("ScriptInterface", "Favourite Adapter not null.");
        } else {
          Log.i("ScriptInterface", "Favourite Adapter null.");
        }
        favourite_list.setAdapter(favouritesAdapter);

        TabHost tabHost=(TabHost)findViewById(R.id.tabHost);
        tabHost.setup();
        
        TabHost.TabSpec spec1=tabHost.newTabSpec("Tab 1");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Write Program");
        
        TabHost.TabSpec spec2=tabHost.newTabSpec("Tab 2");
        spec2.setIndicator("Queue");
        spec2.setContent(R.id.tab2);
        
        TabHost.TabSpec spec3=tabHost.newTabSpec("Tab 3");
        spec3.setIndicator("Favourites Tab");
        spec3.setContent(R.id.tab3);

        /*TabHost.TabSpec spec4=tabHost.newTabSpec("Tab 4");
        spec4.setIndicator("Tab 4");
        spec4.setContent(R.id.tab4);
        */
        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);
        //tabHost.addTab(spec4);
        
  }

  @Override
  protected void onNodeCreate(Node node) {
    super.onNodeCreate(node);
    getQueue();
  }
  
  @Override
  protected void onNodeDestroy(Node node) {
    /*for (int i = 0; i < my_programs.size(); i++) {
      db.execSQL("INSERT INTO " +
                        TABLE_NAME +
                        " Values (" + my_programs.get(i).id.toString() + " );");
    } 
    */
    super.onNodeDestroy(node);
  }
 
  public class FavouritesAdapter extends BaseAdapter { 

        public FavouritesAdapter() {          
           //super(ScriptInterface.this, R.layout.queue_item);
        }

    public int getCount() {
        return favourite_programs.size();
    }

    public long getItemId(int position) {
        return position;
    }

    public Object getItem(int position) {
        return favourite_programs.get(position);
    }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            Log.i("ScriptInterface", "Getting View!");
            convertView = null;
            //LayoutInflater inflater = getLayoutInflater();

            
            //row = inflater.inflate(R.layout.queue_item, parent, false);

            if (convertView == null) {
              LayoutInflater inflater = (LayoutInflater) ScriptInterface.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
              convertView = inflater.inflate(R.layout.favourite_item, null);
            }

            final ProgramInfo info = favourite_programs.get(position);

            TextView f_name = (TextView) convertView.findViewById(R.id.f_name_row);
            f_name.setText(info.name);
            TextView f_owner = (TextView) convertView.findViewById(R.id.f_owner_row);
            f_owner.setText(info.owner);
            Button f_queue_btn = (Button) convertView.findViewById(R.id.f_queue_btn);

            ImageButton imageButton = (ImageButton) convertView.findViewById(R.id.favourite_btn);
            if (favourite_programs.contains(info)) {
              imageButton.setImageDrawable(ScriptInterface.this.getResources().getDrawable(R.drawable.r1));
            } else {
              imageButton.setImageDrawable(ScriptInterface.this.getResources().getDrawable(R.drawable.w1));
            }

            //set the click listener
            imageButton.setOnClickListener(new OnClickListener() {

              public void onClick(View button) {
                  favourite_programs.remove(info);
                  FavouritesAdapter favouritesAdapter = new FavouritesAdapter();
                  favourite_list.setAdapter(favouritesAdapter);
                  favouritesAdapter.notifyDataSetChanged();
                  getQueue();
                /*if (button.isSelected()){
                  button.setSelected(false);
                  favourite_programs.remove(info);
                  //...Handle toggle off
                } else {
                  button.setSelected(true);
                  favourite_programs.add(info);
                  //...Handled toggle on
                }*/
             }

            });


            f_queue_btn.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        
                        queueProgram(info.id);
                        //call getQueue() in delete service
                    }
                }
            );

            

            return(convertView);
        }
  } 

  public class QueueAdapter extends BaseAdapter { 

        public QueueAdapter() {          
           //super(ScriptInterface.this, R.layout.queue_item);
        }

    public int getCount() {
        return program_queue.size();
    }

    public long getItemId(int position) {
        return position;
    }

    public Object getItem(int position) {
        return program_queue.get(position);
    }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            Log.i("ScriptInterface", "Getting View!");
            convertView = null;
            //LayoutInflater inflater = getLayoutInflater();

            
            //row = inflater.inflate(R.layout.queue_item, parent, false);

            if (convertView == null) {
              LayoutInflater inflater = (LayoutInflater) ScriptInterface.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
              convertView = inflater.inflate(R.layout.queue_item, null);
            }

            final ProgramInfo info = program_queue.get(position);

            TextView name = (TextView) convertView.findViewById(R.id.name_row);
            name.setText(info.name);
            TextView owner = (TextView) convertView.findViewById(R.id.owner_row);
            owner.setText(info.owner);
            Button delete_btn = (Button) convertView.findViewById(R.id.delete_btn);
            Button run_btn = (Button) convertView.findViewById(R.id.run_btn);

            ImageButton imageButton = (ImageButton) convertView.findViewById(R.id.favourite_btn);
            if (favourite_programs.contains(info)) {
              imageButton.setImageDrawable(ScriptInterface.this.getResources().getDrawable(R.drawable.r1));
            } else {
              imageButton.setImageDrawable(ScriptInterface.this.getResources().getDrawable(R.drawable.w1));
            }

            //set the click listener
            imageButton.setOnClickListener(new OnClickListener() {

              public void onClick(View button) {
                if (favourite_programs.contains(info)){
                  button.setSelected(false);
                  favourite_programs.remove(info);
                  //...Handle toggle off
                } else {
                  button.setSelected(true);
                  favourite_programs.add(info);
                  //...Handled toggle on
                }
                FavouritesAdapter favouritesAdapter = new FavouritesAdapter();
                favourite_list.setAdapter(favouritesAdapter);
                favouritesAdapter.notifyDataSetChanged();
                getQueue();
             }

            });



            delete_btn.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        
                        dequeue(info.id);
                        //call getQueue() in delete service
                    }
                }
            );
            run_btn.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        runProgram(info.id);
                        //call getQueue() in delete service
                    }
                }
            );

            

            return(convertView);
        }
  }

  public void dequeue(long id) {
    Log.i("ScriptInterface", "Run: DequeueProgram");
    try {
      ServiceClient<DequeueProgram.Request, DequeueProgram.Response> appServiceClient =
        getNode().newServiceClient("/dequeue_program", "program_queue/DequeueProgram");
      DequeueProgram.Request appRequest = new DequeueProgram.Request();
      appRequest.token = token;
      appRequest.id = id;
      appServiceClient.call(appRequest, new ServiceResponseListener<DequeueProgram.Response>() {
          @Override public void onSuccess(DequeueProgram.Response message) {
            getQueue();
          }

          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }
  
  }

  public void runProgram(long id) {
    Log.i("ScriptInterface", "Run: runProgram");
    try {
      ServiceClient<RunProgram.Request, RunProgram.Response> appServiceClient =
        getNode().newServiceClient("/run_program", "program_queue/RunProgram");
      RunProgram.Request appRequest = new RunProgram.Request();
      appRequest.token = token;
      appRequest.id = id;
      appServiceClient.call(appRequest, new ServiceResponseListener<RunProgram.Response>() {
          @Override public void onSuccess(RunProgram.Response message) {
            getQueue();
          }

          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }

  }

  private void stopProgress() {
    final ProgressDialog temp = progress;
    progress = null;
    if (temp != null) {
      runOnUiThread(new Runnable() {
          public void run() {
            temp.dismiss();
          }});
    }
  }

  public void updateQueue(View view) {
    getQueue();
  }
 
  private void getProgram(long id) {
    Log.i("ScriptInterface", "Run: GetProgram");
    try {
      ServiceClient<GetProgram.Request, GetProgram.Response> appServiceClient =
        getNode().newServiceClient("/get_program", "program_queue/GetProgram");  
      GetProgram.Request appRequest = new GetProgram.Request();
      appRequest.id = id;
      appServiceClient.call(appRequest, new ServiceResponseListener<GetProgram.Response>() {
          @Override public void onSuccess(GetProgram.Response message) {
            current_program = message.program;
            Log.i("ScriptInterface", "ID is : " + String.valueOf(message.program.info.id));
            final String code = message.program.code;
            Log.i("ScriptInterface", "Program is: " + code);
            ScriptInterface.this.runOnUiThread(new Runnable() {
              public void run() {
              // TODO Auto-generated method stub
              program_field.setText(code);
              }
            });
          }

          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }
  }

  private void getQueue() {
    Log.i("ScriptInterface", "Run: GetQueue");
    try {
      ServiceClient<GetQueue.Request, GetQueue.Response> appServiceClient =
        getNode().newServiceClient("/get_queue", "program_queue/GetQueue");
      GetQueue.Request appRequest = new GetQueue.Request();
      appServiceClient.call(appRequest, new ServiceResponseListener<GetQueue.Response>() {
          @Override public void onSuccess(GetQueue.Response message) {
            //stopProgress();
            program_queue = message.programs;
            queue_names.clear();
            for (int i = 0; i < program_queue.size(); i++) {
              Log.i("ScriptInterface", "Queue contains: " + program_queue.get(i).name);
              queue_names.add(program_queue.get(i).name);
            }
            ScriptInterface.this.runOnUiThread(new Runnable() {
              public void run() {
              Log.i("ScriptInterface", "Dataset changed!");
              QueueAdapter queueAdapter = new QueueAdapter();
              list.setAdapter(queueAdapter);
              queueAdapter.notifyDataSetChanged();
              }
            });
          }

          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }  
  }
  private void getMyPrograms() {
    Log.i("ScriptInterface", "Run: GetMyPrograms");
    try {
      ServiceClient<GetMyPrograms.Request, GetMyPrograms.Response> appServiceClient =
        getNode().newServiceClient("/get_my_programs", "program_queue/GetMyPrograms");  
      GetMyPrograms.Request appRequest = new GetMyPrograms.Request();
      appRequest.token = token;
      appServiceClient.call(appRequest, new ServiceResponseListener<GetMyPrograms.Response>() {
          @Override public void onSuccess(GetMyPrograms.Response message) {
            //stopProgress();
            my_programs = message.programs;
            ScriptInterface.this.runOnUiThread(new Runnable() {
              public void run() {
              // TODO Auto-generated method stub
              showDialog(EXISTING_PROGRAM_DIALOG);

              }  
            });
          }
        
          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }
  }

  private void updateProgram() {
    Log.i("ScriptInterface", "Run: UpdateProgram");
    current_program.code = program_field.getText().toString();
    current_program.info.name = name_field.getText().toString();
    current_program.info.type = (byte) type;
    current_program.info.owner = username;
    
    try {
      ServiceClient<UpdateProgram.Request, UpdateProgram.Response> appServiceClient =
        getNode().newServiceClient("/update_program", "program_queue/UpdateProgram");  
      UpdateProgram.Request appRequest = new UpdateProgram.Request();
      appRequest.token = token;
      appRequest.program = current_program;
      appServiceClient.call(appRequest, new ServiceResponseListener<UpdateProgram.Response>() {
          @Override public void onSuccess(UpdateProgram.Response message) {
          }

          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }
    
  }
 
  private void createProgram() {
    current_program =  new Program();
    current_program.code = program_field.getText().toString();
    current_program.info.name = name_field.getText().toString();
    current_program.info.type = (byte) type;
    current_program.info.owner = username;
    
    Log.i("ScriptInterface", "Run: CreateProgram");
    try {
      ServiceClient<CreateProgram.Request, CreateProgram.Response> appServiceClient =
        getNode().newServiceClient("/create_program", "program_queue/CreateProgram"); 
      CreateProgram.Request appRequest = new CreateProgram.Request();
      appRequest.token = token;
      appServiceClient.call(appRequest, new ServiceResponseListener<CreateProgram.Response>() {
          @Override public void onSuccess(CreateProgram.Response message) {
            current_program.info.id = message.id;
            updateProgram();
          }

          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }
  }

  public void clearQueue(View view){
    Log.i("ScriptInterface", "Run: ClearQueue");
    try {
      ServiceClient<ClearQueue.Request, ClearQueue.Response> appServiceClient =
        getNode().newServiceClient("/clear_queue", "program_queue/ClearQueue");
      ClearQueue.Request appRequest = new ClearQueue.Request();
      appRequest.token = token;
      appServiceClient.call(appRequest, new ServiceResponseListener<ClearQueue.Response>() {
          @Override public void onSuccess(ClearQueue.Response message) {
            getQueue();            
          }

          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }
  }

  @Override 
  public void onBackPressed() {
    if (getIntent().getStringExtra("activity") != null) {
      Class<?> activityClass = null;
      Intent intent = new Intent();
      try {
        activityClass = Class.forName(getIntent().getStringExtra("activity"));
        intent = new Intent(ScriptInterface.this, activityClass);
      } catch (ClassNotFoundException e) {
        intent = ScriptInterface.this.getPackageManager().getLaunchIntentForPackage("org.ros.android.app_chooser");
      }
      intent.setAction("android.intent.action.MAIN");
      intent.addCategory("android.intent.category.LAUNCHER");
      intent.addCategory("android.intent.category.DEFAULT");
      startActivity(intent);
    } else {
      Intent intent = new Intent();
      intent = ScriptInterface.this.getPackageManager().getLaunchIntentForPackage("org.ros.android.app_chooser");
      intent.setAction("android.intent.action.MAIN");
      intent.addCategory("android.intent.category.LAUNCHER");
      intent.addCategory("android.intent.category.DEFAULT");
      startActivity(intent);
    }
  }




  @Override
  protected Dialog onCreateDialog(int id) {
    final Dialog dialog;
    switch (id) {
      case EXISTING_PROGRAM_DIALOG:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (my_programs.size() > 0) { 
              builder.setTitle("Select a Progam to Edit");
          String[] program_names = new String[my_programs.size()];
          for (int i = 0; i < my_programs.size(); i++) {
            program_names[i] = my_programs.get(i).name;
          }
          builder.setSingleChoiceItems(program_names, 0, null)
           .setPositiveButton("Edit Selected", new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                showProgram(my_programs.get(selectedPosition));
             }
           });
         builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
             removeDialog(EXISTING_PROGRAM_DIALOG);
           }
         });
         builder.setNeutralButton("Add to Queue", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
             int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
             removeDialog(EXISTING_PROGRAM_DIALOG);
             ProgramInfo info = my_programs.get(selectedPosition);
             queueProgram(info.id);
           }
         });
         dialog = builder.create();
         }
       else {
         builder.setTitle("No Programs to Edit. Create a New Program.");
         dialog = builder.create();
         final Timer t = new Timer();
         t.schedule(new TimerTask() {
           public void run() {
             removeDialog(EXISTING_PROGRAM_DIALOG);
           }
         }, 3*1000);
       }
        break;
      default:
        dialog = null;
    }
    return dialog;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.options_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.logout) {
      
      Intent intent = new Intent(this, LoginActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }
  
  public void logout() {
    Log.i("ScriptInterface", "Run: Logout");
    try {
      ServiceClient<Logout.Request, Logout.Response> appServiceClient =
        getNode().newServiceClient("/logout", "program_queue/Logout");  
      Logout.Request appRequest = new Logout.Request();
      appRequest.token = token;
      appServiceClient.call(appRequest, new ServiceResponseListener<Logout.Response>() {
          @Override public void onSuccess(Logout.Response message) {
            Intent intent = new Intent(ScriptInterface.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
          }

          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("ScriptInterface", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("ScriptInterface", e.toString());
    }
  }

  public void showProgram(ProgramInfo info) {
    //get actual program from program info 
    //switch program type field, enter program name, put text in edit text
    getProgram(info.id); 
    name_field.setText(info.name);
    if (current_program == null) {
      current_program = new Program();
    }
    current_program.info.name = info.name;
    if (info.type == PYTHON) {
      spinner.setSelection(0);
      type = PYTHON;
    } else if (info.type == PUPPETSCRIPT) {
      spinner.setSelection(1);
      type = PUPPETSCRIPT;
    }
     
  }

  public void saveProgram(View view) {
    if (current_program != null) {
      Log.i("ScriptInterface", "Current program name: " + current_program.info.name);
      Log.i("ScriptInterface", "Current program name: " + name_field.getText().toString());
      if (name_field.getText().toString().equals(current_program.info.name)) {
        Log.i("ScriptInterface", "Names match.");
        updateProgram();
      } else if (name_field.getText().toString() == "") {
        //alert dialog about needing name
      } else {
        //you are about to create a new program
        Log.i("ScriptInterface", "Names don't match.");
        createProgram();
      }
      
    } else {
      Log.i("ScriptInterface", "Program is null.");
      createProgram();
    }
  }

  public void addToQueue(View view) {
    //check if saved, if not prompt to save
    Log.i("ScriptInterface", "Current Program code: " + current_program.code);
    Log.i("ScriptInterface", "Text Field code: " + program_field.getText().toString());
    if (current_program.code.equals(program_field.getText().toString())) {
      Log.i("ScriptInterface", "Run: QueueProgram");
      saveProgram(findViewById(android.R.id.content));
      queueProgram(current_program.info.id );
    } 
    
  }

  public void queueProgram(long id) {
      try {
        ServiceClient<QueueProgram.Request, QueueProgram.Response> appServiceClient =
          getNode().newServiceClient("/queue_program", "program_queue/QueueProgram");  
        QueueProgram.Request appRequest = new QueueProgram.Request();
        appRequest.token = token;
        appRequest.program_id = id;
        appServiceClient.call(appRequest, new ServiceResponseListener<QueueProgram.Response>() {
            @Override public void onSuccess(QueueProgram.Response message) {
              //tell user which position their item is in the queue, message.queue_position
              getQueue();
            }
            @Override public void onFailure(RemoteException e) {
              //TODO: SHOULD ERROR
              Log.e("ScriptInterface", e.toString());
            }
         });
      } catch (Exception e) {
        //TODO: should error
        Log.e("ScriptInterface", e.toString());
      }

  }

  public class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
    @Override
    public void onItemSelected(AdapterView<?> parent,
        View view, int pos, long id) {
          if (pos == 0) {
            type = PYTHON;
          } else if (pos == 1) {
            type = PUPPETSCRIPT;
          }
    }
    @Override
    public void onNothingSelected(AdapterView parent) {
      // Do nothing.
    }

  }



}
