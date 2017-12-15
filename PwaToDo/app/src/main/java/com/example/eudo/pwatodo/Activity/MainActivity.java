package com.example.eudo.pwatodo.Activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.eudo.pwatodo.Adapter.ListItemAdapter;
import com.example.eudo.pwatodo.Model.ToDo;
import com.example.eudo.pwatodo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dmax.dialog.SpotsDialog;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class MainActivity extends AppCompatActivity{

    List<ToDo> toDoList = new ArrayList<>();
    FirebaseFirestore db;

    RecyclerView listItem;
    RecyclerView.LayoutManager layoutManager;

    FloatingActionButton fab;

    public MaterialEditText title, description;
    public boolean isUpdate = false; //флаг который смотрит обновлнеие ли это или новый элемент
    public String idUpdate = ""; //id элемента который нуждается в обновлении
    private String userEmail;
    private String idUser;

    ListItemAdapter adapter;

    Spinner category, spinner;

    android.app.AlertDialog dialog;

    List<String> categoryList;
    String[] categoryStandart = {"Все категории", "Личное", "Покупки", "Работа"};
    String selected, spinner_item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) findViewById(R.id.searchToDO);

        //Получаем логин и id пользователя
        Intent intent = getIntent();
        idUser = intent.getStringExtra("idUser");
        userEmail = intent.getStringExtra("email");

        //Инициализация FireStore
        db = FirebaseFirestore.getInstance();

        //View
        dialog = new SpotsDialog(this);
        title = (MaterialEditText) findViewById(R.id.title);
        description = (MaterialEditText) findViewById(R.id.description);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        category = (Spinner) findViewById(R.id.categorySelection);
        spinner = (Spinner) findViewById(R.id.spinner);

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                category.setSelection(0);
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                List<ToDo> search = new ArrayList<>();
                for (int i = 0; i < toDoList.size(); i++) {
                    if (toDoList.get(i).getTitle().contains(query)
                            || toDoList.get(i).getDescription().contains(query)) {
                        search.add(toDoList.get(i));
                    }
                }
                adapter = new ListItemAdapter(MainActivity.this, search);
                listItem.setAdapter(adapter);
                dialog.dismiss();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText == null) {
                    adapter = new ListItemAdapter(MainActivity.this, toDoList);
                    listItem.setAdapter(adapter);
                    dialog.dismiss();
                } else {
                    List<ToDo> search = new ArrayList<>();
                    for (int i = 0; i < toDoList.size(); i++) {
                        if (toDoList.get(i).getTitle().contains(newText)
                                || toDoList.get(i).getDescription().contains(newText)) {
                            search.add(toDoList.get(i));
                        }
                    }
                    adapter = new ListItemAdapter(MainActivity.this, search);
                    listItem.setAdapter(adapter);
                    dialog.dismiss();
                }

                return false;
            }
        });

        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                List<ToDo> categorySelect = new ArrayList<>();
                dialog.show();
                if (category.getSelectedItem().equals("Все категории")) {
                    adapter = new ListItemAdapter(MainActivity.this, toDoList);
                    listItem.setAdapter(adapter);
                    dialog.dismiss();
                } else {
                    for (int i = 0; i < toDoList.size(); i++) {
                        if (toDoList.get(i).getTitle().equals(category.getSelectedItem()))
                            categorySelect.add(toDoList.get(i));
                        adapter = new ListItemAdapter(MainActivity.this, categorySelect);
                        listItem.setAdapter(adapter);
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinner.getSelectedItem().equals("Все категории")) title.setText("Личное");
                else
                title.setText(spinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Добавить новый ToDo
                if (!isUpdate) {
                    if (description.getText().equals("Введите задачу")) {
                    } else
                    setData(title.getText().toString(), description.getText().toString());
                } else {
                    updateData(title.getText().toString(), description.getText().toString());
                    isUpdate = !isUpdate;
                }
            }
        });

        listItem = (RecyclerView) findViewById(R.id.listTodo);
        listItem.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        listItem.setLayoutManager(layoutManager);

        loadData(); //Читаем данные из FireStore

    }

    private static long back_pressed;

    @Override
    public void onBackPressed() {
        category = (Spinner) findViewById(R.id.categorySelection);
        if (category.getSelectedItem() != "Все категории") {
            category.setSelection(0);
        }

        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), "Press once again to exit!",
                    Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals("DELETE"))
            deleteItem(item.getOrder());
        return super.onContextItemSelected(item);
    }

    private void deleteItem(int index) {
        db.collection("Users").document(idUser).collection("ToDoList")
                .document(toDoList.get(index).getId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        loadData();
                    }
                });
    }

    private void updateData(String title, String description) {
        db.collection("Users").document(idUser).collection("ToDoList").document(idUpdate)
                .update("title", title, "description", description)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Updated !", Toast.LENGTH_SHORT).show();
                    }
                });

        //Обновление данных в реальном времени
        db.collection("ToDoList").document(idUpdate)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                        loadData();
                    }
                });
    }

    private void setData(String title, String description) {
        //Случайное id
        String id = UUID.randomUUID().toString();
        Map<String, Object> todo = new HashMap<>();
        todo.put("id", id);
        todo.put("title", title);
        todo.put("description", description);

        db.collection("Users")
                .document(idUser)
                .collection("ToDoList")
                .document(id)
                .set(todo).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //Обновляем данные
                loadData();
            }
        });
    }

    private void loadData() {
        dialog.show();
        // categoryList.clear();
        categoryList = new ArrayList<String>(Arrays.asList(categoryStandart));
        category = (Spinner) findViewById(R.id.categorySelection);
        spinner = (Spinner) findViewById(R.id.spinner);

        if (toDoList.size() > 0) toDoList.clear(); //Перписываем старые значения
        db.collection("Users")
                .document(idUser)
                .collection("ToDoList")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            final ToDo todo = new ToDo(doc.getString("id"),
                                    doc.getString("title"),
                                    doc.getString("description"));

                            boolean checkList = false;
                            for (int i = 0; i < categoryList.size(); i++)
                                if (categoryList.get(i).equals(todo.getTitle()))
                                    checkList = true;
                            if (!checkList)
                                categoryList.add(todo.getTitle());
                            toDoList.add(todo);
                        }

                        updateCategory(spinner, categoryList);
                        updateCategory(category, categoryList);

                        adapter = new ListItemAdapter(MainActivity.this, toDoList);
                        listItem.setAdapter(adapter);
                        dialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateCategory(Spinner _spinner, List<String> list) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        _spinner.setAdapter(adapter);
    }

    public void clickLogoutButton(View view) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();
        Intent intent = new Intent(this, EmailPasswordActivity.class);
        finish();
        startActivity(intent);
    }

    public void clickTitle(View view) {
        title.setText("");
    }

    public void clickDescription(View view) {
        description.setText("");
    }
}