package com.group18.app.calendar;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.group18.app.calendar.database.CommitmentHelper;
import com.group18.app.calendar.database.CommitmentSchema;
import com.group18.app.calendar.database.ReminderHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

//this is the Activity that is launched when app is started, see manifest file
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, DeleteDialogFragment.InterfaceCommunicator, addClassFragment.CheckDuplicate{

    private DrawerLayout myDrawerLayout;
    public static ArrayList<Commitments> myCommits = new ArrayList<>();
    public ArrayList<Reminders> myReminders = new ArrayList<>();
    private boolean mScheduleVisible = true;
    private static final String SAVED_SCHEDULE_VISIBLE = "schedule";
    private static final int AddClassCode = 0; //code used to identify result information coming from AddClassActivity
    private static final int DeleteCommitmentCode = 1;
    private static final int DeleteReminderCode = 5;
    private static final int AddReminderCode = 2;
    private CommitmentHelper mCommitmentDbHelper;
    private ReminderHelper mReminderDbHelper;
    private SQLiteDatabase mCommitmentDatabase;
    private SQLiteDatabase mReminderDatabase;
    private RecyclerView mRecyclerView;
    private CommitmentsAdapter dailyClassAdapter;
    private NavigationView myNavView;
    public static TextView welcome;
    private RecyclerView mRecyclerView2;
    private RemindersAdapter mAdapter2;
    public static boolean first_day_monday = false;
    public static String username;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//upon rotation, activity is recreated, retrieve icon status from savedInstanceState
        if(savedInstanceState != null){
            mScheduleVisible = savedInstanceState.getBoolean(SAVED_SCHEDULE_VISIBLE);
        }
        Global.getInstance().setContext(this);
        super.onCreate(savedInstanceState);
        SharedPreferences pref = getSharedPreferences("queryname", MODE_PRIVATE);
        boolean askForName = pref.getBoolean("Name", false);

        if(!askForName)
            showDialog();

        mCommitmentDbHelper = new CommitmentHelper(getApplicationContext());
        mReminderDbHelper = new ReminderHelper(getApplicationContext());
        mCommitmentDatabase = mCommitmentDbHelper.getWritableDatabase();
        mReminderDatabase = mReminderDbHelper.getWritableDatabase();
        setContentView(R.layout.navigation_drawer);
        Toolbar mytoolbar = findViewById(R.id.mytoolbar);
        setSupportActionBar(mytoolbar);
        mytoolbar.setTitle(R.string.app_name);
        mRecyclerView = findViewById(R.id.my_recycler_view);
        //this improves performance of RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (myCommits.isEmpty())
            LoadCommitmentDatabase();
        LoadReminderDatabase();
        Log.i("hello", myReminders.size() + "");
        dailyClassAdapter = new CommitmentsAdapter(MainActivity.this, myCommits);

        dailyClassAdapter.setonItemClickListener(new CommitmentsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //android.app.FragmentManager mFragmentManager = getFragmentManager();
                DeleteDialogFragment dialog = new DeleteDialogFragment();
                Bundle mybundle = new Bundle();
                mybundle.putInt("position", position);
                dialog.setArguments(mybundle);
                dialog.show(getFragmentManager(), "deleteCommitment");
            }
        });


        mRecyclerView.setAdapter(dailyClassAdapter);





        //Reminders adapter and RecyclerView
        //this whole commented out section is for the second recycler view

        mRecyclerView2 = findViewById(R.id.my_recycler_view2);

        //this improves performance of RecyclerView
        mRecyclerView2.setHasFixedSize(true);
        mRecyclerView2.setLayoutManager(new LinearLayoutManager(this));
        mAdapter2 = new RemindersAdapter(MainActivity.this, myReminders);

        mAdapter2.setonItemClickListener(new RemindersAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //android.app.FragmentManager mFragmentManager = getFragmentManager();
                DeleteDialogFragment dialog = new DeleteDialogFragment();
                Bundle mybundle = new Bundle();
                mybundle.putString("ReminderAdapter", "true");
                mybundle.putInt("position", position);
                dialog.setArguments(mybundle);
                dialog.show(getFragmentManager(), "deleteCommitment");
            }
        });

        mRecyclerView2.setAdapter(mAdapter2);


        myNavView = findViewById(R.id.nav_view);
        myNavView.setNavigationItemSelectedListener(this);
        myDrawerLayout = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,myDrawerLayout, mytoolbar,R.string.open_drawer,R.string.close_drawer);
        myDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        welcome = myNavView.getHeaderView(0).findViewById(R.id.nav_header_string);
        username = pref.getString("username", "John Doe");
        welcome.setText(getResources().getString(R.string.welcome_name).replace("%s",username));

        Button startaddClass = findViewById(R.id.start_add_class);
        startaddClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddClassActivity.class);
                startActivityForResult(intent,AddClassCode);
            }
        });

        Button startReminder = findViewById(R.id.start_reminder);
        startReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddReminder.class);
                startActivityForResult(intent, AddReminderCode);
            }
        });



    }

    private void showDialog() {
        SharedPreferences prefs = getSharedPreferences("queryname",MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_query_name, null);
        EditText name = mView.findViewById(R.id.user_name);
        mBuilder.setView(mView);
        Button saveName = mView.findViewById(R.id.name_button);
        AlertDialog dialog = mBuilder.create();

        saveName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(name.getText().toString().isEmpty())
                    name.setError("Enter name to proceed");
                else
                {
//                    editor.putString("username",name.getText().toString());
//                    dialog.dismiss();
                    editor.putString("username",name.getText().toString());
                    editor.apply();
                    dialog.dismiss();
                    welcome.setText(getResources().getString(R.string.welcome_name).replace("%s",name.getText().toString()));
                    editor.putBoolean("Name",true);
                    editor.apply();
                }
            }
        });

        dialog.show();
        //so the user now can't press the back key to get out of this dialog
        dialog.setCancelable(false);
        //so the user now can't click outside of this dialog to dismiss it
        dialog.setCanceledOnTouchOutside(false);
//        editor.putBoolean("Name",false);
//        editor.apply();
    }

    //called when Activity is being destroyed and relevant data should be saved
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save icon status (which is one is viewable)
        outState.putBoolean(SAVED_SCHEDULE_VISIBLE, mScheduleVisible);
    }

    //called before menu is shown
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(!mScheduleVisible)
            menu.findItem(R.id.schedule).setIcon(R.drawable.calendar);
        return super.onPrepareOptionsMenu(menu);
    }

    //create Optionsmenu on toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //what do we do if an item on the menu bar is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.nav:
                Intent intent = new Intent (this, MapActivity.class);
                startActivity(intent);
                break;
            case R.id.schedule:
                Intent intent1 = new Intent(MainActivity.this, WeeklyView.class);
                startActivity(intent1);
                mScheduleVisible = !mScheduleVisible;
                break;

        }
        return super.onOptionsItemSelected(item);
    }

//what do we do if an option is selected on the navigation drawer, answer below
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch(id){
            case R.id.home:
                Toast.makeText(this,"Home",Toast.LENGTH_SHORT).show();
                break;
            case R.id.cal_view:
                Intent cal = new Intent(MainActivity.this, WeeklyView.class);
                startActivity(cal);
                break;
            case R.id.classes:
                Toast.makeText(this,"Classes",Toast.LENGTH_SHORT).show();
                break;
            case R.id.tasks:
                Toast.makeText(this,"Reminders",Toast.LENGTH_SHORT).show();
                break;
            case R.id.settings_id:
                Intent intent = new Intent (this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.Help_id:
                Intent intent1 = new Intent (this, HelpActivity.class);
                startActivity(intent1);
                break;
        }
        myDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    //closes drawer after an option is pressed on navigation drawer or if area not on navigation drawer is pressed
    @Override
    public void onBackPressed() {
        if(myDrawerLayout.isDrawerOpen(GravityCompat.START)){
            myDrawerLayout.closeDrawer(GravityCompat.START);
        }
        else
        super.onBackPressed();
    }

    @Override //Function called after a commitment is committed. It adds the commitment to the myCommits array
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
            //if we receive a bad result from the activity, do nothing
            if(resultCode != Activity.RESULT_OK)
                return;

            //if the activity reporting back is AddClassActivity, do the following
            if(requestCode == AddClassCode){
                Commitments tempclass =  data.getParcelableExtra("retrieveUFClass");
                myCommits.add(tempclass);
                mCommitmentDatabase.insert(CommitmentSchema.CommitmentTable.NAME, null, getContentValues(tempclass));
                int should_i_notify = daily_class_view_add_notification(tempclass);
                if (should_i_notify != -1){
                    dailyClassAdapter.notifyItemInserted(should_i_notify);
                }

            }
            if(requestCode == AddReminderCode) {
                Reminders reminder = data.getParcelableExtra("reminderObj");
                myReminders.add(reminder);
                mReminderDatabase.insert(CommitmentSchema.ReminderTable.NAME,null,getContentValues(reminder));
                mAdapter2.notifyItemInserted(myReminders.size()-1);
                Log.i("hello", "reminder = " + reminder.getName());

            }
    }


    /*
    method will take a commitment object and insert it into a ContentValues object,
    it will return a ContentValues object that is ready to be inserted into the database
    */
    private int daily_class_view_add_notification(Commitments temp){
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        Date currentDate = calendar.getTime();
        if ((currentDate.after(temp.getStart()) && currentDate.before(temp.getEnd())) || currentDate.equals(temp.getStart()) || currentDate.equals(temp.getEnd())) { //If we are within range of the dates
            if (temp.getOnTheseDays().contains("Monday") && Calendar.MONDAY == day) { //If today is monday and the class is on mondays
                if (dailyClassAdapter.daily_commitments.isEmpty()){
                    dailyClassAdapter.daily_commitments.add(0,temp);
                    return 0;
                } else {
                    for (int k = 0; k < dailyClassAdapter.daily_commitments.size(); k++) {
                        //is called when daily commitments hour is earlier than the new temp class we are trying to add
                        if (dailyClassAdapter.daily_commitments.get(k).getStartHour() > temp.getStartHour() || (dailyClassAdapter.daily_commitments.get(k).getStartHour() == temp.getStartHour() && dailyClassAdapter.daily_commitments.get(k).getStartMinute() > temp.getStartMinute())) {
                            dailyClassAdapter.daily_commitments.add(k, temp);
                            return k;
                        }
                        if (k == dailyClassAdapter.daily_commitments.size() - 1) {
                            dailyClassAdapter.daily_commitments.add(k + 1, temp);
                            return k + 1;
                        }
                    }
                }
            }
            else if (temp.getOnTheseDays().contains("Tuesday") && Calendar.TUESDAY == day) { //If today is monday and the class is on mondays
                if (dailyClassAdapter.daily_commitments.isEmpty()){
                    dailyClassAdapter.daily_commitments.add(0,temp);
                    return 0;
                } else {
                    for (int k = 0; k < dailyClassAdapter.daily_commitments.size(); k++) {
                        //is called when daily commitments hour is earlier than the new temp class we are trying to add
                        if (dailyClassAdapter.daily_commitments.get(k).getStartHour() > temp.getStartHour() || (dailyClassAdapter.daily_commitments.get(k).getStartHour() == temp.getStartHour() && dailyClassAdapter.daily_commitments.get(k).getStartMinute() > temp.getStartMinute())) {
                            dailyClassAdapter.daily_commitments.add(k, temp);
                            return k;
                        }
                        if (k == dailyClassAdapter.daily_commitments.size() - 1) {
                            dailyClassAdapter.daily_commitments.add(k + 1, temp);
                            return k + 1;
                        }
                    }
                }
            }
            else if (temp.getOnTheseDays().contains("Wednesday") && Calendar.WEDNESDAY == day) { //If today is monday and the class is on mondays
                if (dailyClassAdapter.daily_commitments.isEmpty()){
                    dailyClassAdapter.daily_commitments.add(0,temp);
                    return 0;
                } else {
                    for (int k = 0; k < dailyClassAdapter.daily_commitments.size(); k++) {
                        //is called when daily commitments hour is earlier than the new temp class we are trying to add
                        if (dailyClassAdapter.daily_commitments.get(k).getStartHour() > temp.getStartHour() || (dailyClassAdapter.daily_commitments.get(k).getStartHour() == temp.getStartHour() && dailyClassAdapter.daily_commitments.get(k).getStartMinute() > temp.getStartMinute())) {
                            dailyClassAdapter.daily_commitments.add(k, temp);
                            return k;
                        }
                        if (k == dailyClassAdapter.daily_commitments.size() - 1) {
                            dailyClassAdapter.daily_commitments.add(k + 1, temp);
                            return k + 1;
                        }
                    }
                }
            }
            else if (temp.getOnTheseDays().contains("Thursday") && Calendar.THURSDAY == day) { //If today is monday and the class is on mondays
                if (dailyClassAdapter.daily_commitments.isEmpty()){
                    dailyClassAdapter.daily_commitments.add(0,temp);
                    return 0;
                } else {
                    for (int k = 0; k < dailyClassAdapter.daily_commitments.size(); k++) {
                        //is called when daily commitments hour is earlier than the new temp class we are trying to add
                        if (dailyClassAdapter.daily_commitments.get(k).getStartHour() > temp.getStartHour() || (dailyClassAdapter.daily_commitments.get(k).getStartHour() == temp.getStartHour() && dailyClassAdapter.daily_commitments.get(k).getStartMinute() > temp.getStartMinute())) {
                            dailyClassAdapter.daily_commitments.add(k, temp);
                            return k;
                        }
                        if (k == dailyClassAdapter.daily_commitments.size() - 1) {
                            dailyClassAdapter.daily_commitments.add(k + 1, temp);
                            return k + 1;
                        }
                    }
                }
            }
            else if (temp.getOnTheseDays().contains("Friday") && Calendar.FRIDAY == day) { //If today is monday and the class is on mondays
                if (dailyClassAdapter.daily_commitments.isEmpty()){
                    dailyClassAdapter.daily_commitments.add(0,temp);
                    return 0;
                } else {
                    for (int k = 0; k < dailyClassAdapter.daily_commitments.size(); k++) {
                        //is called when daily commitments hour is earlier than the new temp class we are trying to add
                        if (dailyClassAdapter.daily_commitments.get(k).getStartHour() > temp.getStartHour() || (dailyClassAdapter.daily_commitments.get(k).getStartHour() == temp.getStartHour() && dailyClassAdapter.daily_commitments.get(k).getStartMinute() > temp.getStartMinute())) {
                            dailyClassAdapter.daily_commitments.add(k, temp);
                            return k;
                        }
                        if (k == dailyClassAdapter.daily_commitments.size() - 1) {
                            dailyClassAdapter.daily_commitments.add(k + 1, temp);
                            return k + 1;
                        }
                    }
                }
            }
            else if (temp.getOnTheseDays().contains("Saturday") && Calendar.SATURDAY == day) { //If today is monday and the class is on mondays
                if (dailyClassAdapter.daily_commitments.isEmpty()){
                    dailyClassAdapter.daily_commitments.add(0,temp);
                    return 0;
                } else {
                    for (int k = 0; k < dailyClassAdapter.daily_commitments.size(); k++) {
                        //is called when daily commitments hour is earlier than the new temp class we are trying to add
                        if (dailyClassAdapter.daily_commitments.get(k).getStartHour() > temp.getStartHour() || (dailyClassAdapter.daily_commitments.get(k).getStartHour() == temp.getStartHour() && dailyClassAdapter.daily_commitments.get(k).getStartMinute() > temp.getStartMinute())) {
                            dailyClassAdapter.daily_commitments.add(k, temp);
                            return k;
                        }
                        if (k == dailyClassAdapter.daily_commitments.size() - 1) {
                            dailyClassAdapter.daily_commitments.add(k + 1, temp);
                            return k + 1;
                        }
                    }

                }
            }
            else if (temp.getOnTheseDays().contains("Sunday") && Calendar.SUNDAY == day) { //If today is monday and the class is on mondays
                if (dailyClassAdapter.daily_commitments.isEmpty()){
                    dailyClassAdapter.daily_commitments.add(0,temp);
                    return 0;
                } else {
                    for (int k = 0; k < dailyClassAdapter.daily_commitments.size(); k++) {
                        //is called when daily commitments hour is earlier than the new temp class we are trying to add
                        if (dailyClassAdapter.daily_commitments.get(k).getStartHour() > temp.getStartHour() || (dailyClassAdapter.daily_commitments.get(k).getStartHour() == temp.getStartHour() && dailyClassAdapter.daily_commitments.get(k).getStartMinute() > temp.getStartMinute())) {
                            dailyClassAdapter.daily_commitments.add(k, temp);
                            return k;
                        }
                        if (k == dailyClassAdapter.daily_commitments.size() - 1) {
                            dailyClassAdapter.daily_commitments.add(k + 1, temp);
                            return k + 1;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private static ContentValues getContentValues(Commitments my_commitment){
        ContentValues values = new ContentValues();
        values.put(CommitmentSchema.CommitmentTable.Cols.PROFESSOR, my_commitment.getProfessor());
        values.put(CommitmentSchema.CommitmentTable.Cols.CNAME, my_commitment.getCname());
        values.put(CommitmentSchema.CommitmentTable.Cols.ID, my_commitment.getPrimarykey().toString());
        values.put(CommitmentSchema.CommitmentTable.Cols.ONTHESEDAYS, my_commitment.getOnTheseDays());
        values.put(CommitmentSchema.CommitmentTable.Cols.START, my_commitment.getStart().toString());
        values.put(CommitmentSchema.CommitmentTable.Cols.END, my_commitment.getEnd().toString());
        values.put(CommitmentSchema.CommitmentTable.Cols.START_HOUR, String.valueOf(my_commitment.getStartHour()));
        values.put(CommitmentSchema.CommitmentTable.Cols.START_MINUTE, String.valueOf(my_commitment.getStartMinute()));
        values.put(CommitmentSchema.CommitmentTable.Cols.END_HOUR, String.valueOf(my_commitment.getEndHour()));
        values.put(CommitmentSchema.CommitmentTable.Cols.END_MINUTE, String.valueOf(my_commitment.getEndMinute()));
        values.put(CommitmentSchema.CommitmentTable.Cols.LAT, String.valueOf(my_commitment.getLat()));
        values.put(CommitmentSchema.CommitmentTable.Cols.LONG, String.valueOf((my_commitment.getLng())));

        return values;
    }




    private static ContentValues getContentValues(Reminders my_reminder){
        ContentValues values = new ContentValues();
        values.put(CommitmentSchema.ReminderTable.Cols.EVENT, my_reminder.getName());
        values.put(CommitmentSchema.ReminderTable.Cols.NOTES, my_reminder.getNotes());
        values.put(CommitmentSchema.ReminderTable.Cols.DATE, my_reminder.getDate().toString());
        values.put(CommitmentSchema.ReminderTable.Cols.HOUR, String.valueOf(my_reminder.getHour()));
        values.put(CommitmentSchema.ReminderTable.Cols.MIN, String.valueOf(my_reminder.getMin()));
        values.put(CommitmentSchema.CommitmentTable.Cols.ID, my_reminder.getPrimarykey().toString());
        return values;
    }

    private void LoadCommitmentDatabase(){

        Cursor cursor = mCommitmentDatabase.query(CommitmentSchema.CommitmentTable.NAME, null,

                null, null, null, null, null);

            try {
                cursor.moveToFirst();
                while(!cursor.isAfterLast()) {

                    String professor = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.PROFESSOR));
                    String cname = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.CNAME));
                    String id = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.ID));
                    String days = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.ONTHESEDAYS));
                    String start = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.START));
                    String end = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.END));

                    String start_hour = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.START_HOUR));
                    String start_minute = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.START_MINUTE));
                    String end_hour = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.END_HOUR));
                    String end_minute = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.END_MINUTE));
                    Double lat = cursor.getDouble((cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.LAT)));
                    Double lng = cursor.getDouble((cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.LONG)));

                    SimpleDateFormat stringformatter = new SimpleDateFormat("E MMM dd HH:mm:ss z YYYY");
                    Date startdate = stringformatter.parse(start);
                    Date enddate = stringformatter.parse(end);

                    Commitments obj1 = new Commitments(professor, cname, days);
                    obj1.setPrimarykey(id);
                    obj1.setStart(startdate);
                    obj1.setEnd(enddate);
                    obj1.setStartHour(Integer.parseInt(start_hour));
                    obj1.setEndHour(Integer.parseInt(end_hour));
                    obj1.setEndMinute(Integer.parseInt(end_minute));
                    obj1.setStartMinute(Integer.parseInt(start_minute));
                    obj1.setLat(lat);
                    obj1.setLng(lng);
                    myCommits.add(obj1);
                    cursor.moveToNext();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
    }

    private void LoadReminderDatabase() {
        Cursor cursor = mReminderDatabase.query(CommitmentSchema.ReminderTable.NAME, null,
                null, null, null, null, null);

        try {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {

                String event = cursor.getString(cursor.getColumnIndex(CommitmentSchema.ReminderTable.Cols.EVENT));
                String notes = cursor.getString(cursor.getColumnIndex(CommitmentSchema.ReminderTable.Cols.NOTES));
                String date = cursor.getString(cursor.getColumnIndex(CommitmentSchema.ReminderTable.Cols.DATE));
                String hour = cursor.getString(cursor.getColumnIndex(CommitmentSchema.ReminderTable.Cols.HOUR));
                String min = cursor.getString(cursor.getColumnIndex(CommitmentSchema.ReminderTable.Cols.MIN));
                String id = cursor.getString(cursor.getColumnIndex(CommitmentSchema.CommitmentTable.Cols.ID));

                SimpleDateFormat stringformatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
                Date startdate = stringformatter.parse(date);

                Reminders obj1 = new Reminders(event,notes);
                obj1.setDate(startdate);
                obj1.setHour(Integer.valueOf(hour));
                obj1.setMin(Integer.valueOf(min));
                obj1.setPrimarykey(id);
                myReminders.add(obj1);
                cursor.moveToNext();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    @Override
    public void sendRequestCode(int code, boolean delete, int position) {
        if(code == DeleteCommitmentCode){
            if(delete && position != -1){
                String primarykey = dailyClassAdapter.daily_commitments.get(position).getPrimarykey().toString();
                dailyClassAdapter.daily_commitments.remove(position);
                dailyClassAdapter.notifyItemRemoved(position);
                for(int i = 0; i<myCommits.size(); i++){
                    if(myCommits.get(i).getPrimarykey().toString().equalsIgnoreCase(primarykey)){
                        myCommits.remove(i);
                    }
                }
                DeletefromDatabase(primarykey,CommitmentSchema.CommitmentTable.NAME);
            }
        }
        else{
            if(delete && position != -1){
                String primarykey = myReminders.get(position).getPrimarykey().toString();
                myReminders.remove(position);
                mAdapter2.notifyItemRemoved(position);
                DeletefromDatabase(primarykey, CommitmentSchema.ReminderTable.NAME);
            }
        }
    }

    public void DeletefromDatabase(String primarykey, String name) {

        if(name == CommitmentSchema.CommitmentTable.NAME) {
            mCommitmentDatabase.delete(CommitmentSchema.CommitmentTable.NAME, CommitmentSchema.CommitmentTable.Cols.ID + " = ?",
                    new String[]{primarykey});
        }
        else{
            mReminderDatabase.delete(CommitmentSchema.ReminderTable.NAME, CommitmentSchema.ReminderTable.Cols.ID + " = ?",
                    new String[]{primarykey});
        }

    }


    //implementing interface method for AddClassFragment
    @Override
    public boolean Check(String classname) {
        for(int i = 0; i < myCommits.size(); ++i){
            if(classname.equals(myCommits.get(i).getCname()))
                return true;
        }
        return false;
    }


}
