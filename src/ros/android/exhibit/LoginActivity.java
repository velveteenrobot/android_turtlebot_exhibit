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
package ros.android.exhibit;

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
import android.view.Menu;
import android.view.View;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import org.ros.service.std_srvs.Empty;
import org.ros.message.trajectory_msgs.JointTrajectory;
import org.ros.message.trajectory_msgs.JointTrajectoryPoint;
import java.util.ArrayList;
import org.ros.message.Duration;

import android.content.Intent;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.ros.service.program_queue.Login;
import org.ros.service.program_queue.Logout;
import org.ros.service.program_queue.CreateUser;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author pratkanis@willowgarage.com (Tony Pratkanis)
 */
public class LoginActivity extends RosAppActivity {
  
  private EditText username_field;
  private EditText pw_field;
  private Button login_btn;
  private Button cancel_btn;
  private Button newuser_btn;
  private long token = 0;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {

    if (getIntent().hasExtra("stop")) {
      Log.i("LoginActivity","Finishing activity...");
      finishActivity(0);
      finish();
    }

    setDefaultAppName("pr2_props_app/pr2_props");
    setDashboardResource(R.id.top_bar);
    setMainWindowResource(R.layout.login_main);
    super.onCreate(savedInstanceState);

    username_field = (EditText) this.findViewById(R.id.username_field);
    //if (savedInstanceState != null) {
    //  username_field.setText(savedInstanceState.getString("LoginText"));
    //  Log.i("LoginActivity", "Getting saved instance state!");
    //}
    pw_field = (EditText) this.findViewById(R.id.pw_field);
    login_btn = (Button) this.findViewById(R.id.login_btn);
    cancel_btn = (Button) this.findViewById(R.id.cancel_btn);
    newuser_btn = (Button) this.findViewById(R.id.newuser_btn);
    login_btn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        //Service call to Login 
        //get token back, switch activities
        login();
      } 
    });
    newuser_btn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        //Service call to CreateUser
        String username = username_field.getText().toString();
        String password = pw_field.getText().toString();
        if (username != "" && password != "") {
        Log.i("LoginActivity", "Run: CreateUser");
        try {
          ServiceClient<CreateUser.Request, CreateUser.Response> appServiceClient =
            getNode().newServiceClient("/create_user", "program_queue/CreateUser");  //TODO: fix package
          CreateUser.Request appRequest = new CreateUser.Request();
          appRequest.name = username_field.getText().toString();
          appRequest.password = pw_field.getText().toString();
          appServiceClient.call(appRequest, new ServiceResponseListener<CreateUser.Response>() {
              @Override 
              public void onSuccess(CreateUser.Response message) {
               if (message.token != 0) {
                 login();
               } else {
                 //should warn about user with same name exists
               }    
              }

              @Override 
              public void onFailure(RemoteException e) {
                //dialog about user with same name exists (most likely)
                Log.e("LoginActivity", e.toString());
              }
          });
        } catch (Exception e) {
          //TODO: should error
          Log.e("LoginActivity", e.toString());
        }
      } else {
        //dialog about having name and password
      }
      }
    });
  }

  @Override
  protected void onNodeCreate(Node node) {
    super.onNodeCreate(node);
  }
  
  @Override
  protected void onNodeDestroy(Node node) {
    super.onNodeDestroy(node);
  }


  private void runService(String service) {
    Log.i("LoginActivity", "Run: " + service);
    try {
      ServiceClient<Empty.Request, Empty.Response> appServiceClient =
        getNode().newServiceClient(service, "std_srvs/Empty");
      Empty.Request appRequest = new Empty.Request();
      appServiceClient.call(appRequest, new ServiceResponseListener<Empty.Response>() {
          @Override public void onSuccess(Empty.Response message) {
          }
        
          @Override public void onFailure(RemoteException e) {
            //TODO: SHOULD ERROR
            Log.e("LoginActivity", e.toString());
          }
        });
    } catch (Exception e) {
      //TODO: should error
      Log.e("LoginActivity", e.toString());
    }
  }

  @Override
  public void onBackPressed() {
      //Intent intent = new Intent();
      Intent intent = getPackageManager().getLaunchIntentForPackage("org.ros.android.app_chooser");
      intent.setAction("android.intent.action.MAIN");
      intent.addCategory("android.intent.category.LAUNCHER");
      intent.addCategory("android.intent.category.DEFAULT");
      //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
      startActivity(intent);
  }

  public void login() {
        final String username = username_field.getText().toString();
        final String password = pw_field.getText().toString();
        if (username != "" && password != "") {
        Log.i("LoginActivity", "Run: Login");
        try {
          ServiceClient<Login.Request, Login.Response> appServiceClient =
            getNode().newServiceClient("/login", "program_queue/Login");  //TODO: fix package
          Login.Request appRequest = new Login.Request();
          appRequest.name = username_field.getText().toString();
          appRequest.password = pw_field.getText().toString();
          appServiceClient.call(appRequest, new ServiceResponseListener<Login.Response>() {
              @Override 
              public void onSuccess(Login.Response message) {
                //Intent intent = getPackageManager().getLaunchIntentForPackage("org.ros.android.exhibit.ExhibitActivity");
                Intent intent = new Intent(LoginActivity.this, ExhibitActivity.class);
                token = (long) message.token;
                if (token != 0) {
                intent.putExtra("username", username);
                intent.putExtra("token", token);
                intent.putExtra("is_admin", message.is_admin);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, 0);
                finish();
                }
              }  

              @Override 
              public void onFailure(RemoteException e) {
                //TODO: SHOULD ERROR
                Log.e("LoginActivity", e.toString());
              }
          });
        } catch (Exception e) {
          //TODO: should error
          Log.e("LoginActivity", e.toString());
        }
        if (token != 0) {
          Toast.makeText(LoginActivity.this, "Login!", Toast.LENGTH_LONG).show();
          //Intent intent = getPackageManager().getLaunchIntentForPackage("org.ros.android.exhibit.ExhibitActivity");
          Intent intent = new Intent(LoginActivity.this, ExhibitActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivityForResult(intent, 0);
          finish();
        }
        } else {
        // dialog "must provide password and username"
        } 
  }

//  @Override
//  public void onSaveInstanceState(Bundle savedInstanceState) {
//    //String s = username_field.getText();
//    savedInstanceState.putString("LoginText", username_field.getText().toString());
//    Log.i("LoginActivity", "Saving Insance state: " + username_field.getText().toString());
//  }




  /* Creates the menu for the options */
  //@Override
  //public boolean onCreateOptionsMenu(Menu menu) {
  //  MenuInflater inflater = getMenuInflater();
  //  inflater.inflate(R.menu.pr2_props_menu, menu);
  //  return true;
  //}

  /* Run when the menu is clicked. */
  //@Override
  //public boolean onOptionsItemSelected(MenuItem item) {
  //  switch (item.getItemId()) {
  //  case R.id.kill: //Shutdown if the user clicks kill
  //    android.os.Process.killProcess(android.os.Process.myPid());
  //    return true;
  //  default:
  //    return super.onOptionsItemSelected(item);
  //  }
  //}

}
