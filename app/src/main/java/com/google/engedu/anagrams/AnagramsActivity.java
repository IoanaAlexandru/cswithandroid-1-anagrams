/* Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.engedu.anagrams;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;


public class AnagramsActivity extends AppCompatActivity {

    public static final String START_MESSAGE = "Find as many words as possible that can be formed by adding one letter to <big>%s</big> (but that do not contain the substring %s).";
    private AnagramDictionary dictionary;
    private String currentWord;
    private List<String> anagrams;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anagrams);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("words.txt");
            dictionary = new AnagramDictionary(new InputStreamReader(inputStream));
        } catch (IOException e) {
            Toast toast = Toast.makeText(this, "Could not load dictionary", Toast.LENGTH_LONG);
            toast.show();
        }
        // Set up the EditText box to process the content of the box when the user hits 'enter'
        final EditText editText = (EditText) findViewById(R.id.editText);
        editText.setRawInputType(InputType.TYPE_CLASS_TEXT);
        editText.setImeOptions(EditorInfo.IME_ACTION_GO);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO || (
                        actionId == EditorInfo.IME_NULL && event != null && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    processWord(editText);
                    handled = true;
                }
                return handled;
            }
        });
        changeScoreTo(0);
    }

    private void changeScoreTo(int newScore) {
        final TextView scoreView = (TextView) findViewById(R.id.scoreView);
        score = newScore;
        scoreView.setText(R.string.score);
        scoreView.append(String.format(Locale.getDefault(), "%d", score));
    }

    private void refreshRemaining() {
        final TextView remainingView = (TextView) findViewById(R.id.remainingView);

        remainingView.setText(R.string.remaining);
        remainingView.append(String.format(Locale.getDefault(), "%d", anagrams.size()));
    }

    private void processWord(EditText editText) {
        TextView resultView = (TextView) findViewById(R.id.resultView);

        String word = editText.getText().toString().trim().toLowerCase();
        if (word.length() == 0) {
            return;
        }
        String color = "#cc0029";
        if (dictionary.isGoodWord(word, currentWord) && anagrams.contains(word)) {
            anagrams.remove(word);
            color = "#00aa29";
            changeScoreTo(score + word.length());
            refreshRemaining();
        } else {
            word = "X " + word;
        }

        resultView.append(Html.fromHtml(String.format("<font color=%s>%s</font><BR>", color, word)));

        editText.setText("");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_anagrams, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_restart) {
            final TextView gameStatus = (TextView) findViewById(R.id.gameStatusView);
            final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            final EditText editText = (EditText) findViewById(R.id.editText);
            final TextView resultView = (TextView) findViewById(R.id.resultView);
            final TextView remainingView = (TextView) findViewById(R.id.remainingView);

            gameStatus.setText(R.string.initial_message);
            fab.setImageResource(android.R.drawable.ic_media_play);
            editText.setText("");
            editText.setEnabled(false);
            resultView.setText("");
            remainingView.setText("");

            changeScoreTo(0);
            dictionary.reset();
            currentWord = null;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void defaultAction(View view) {
        if (currentWord == null) {
            if (dictionary.getUsedWords().size() > 0)
                getDifficultyDialog().show();
            else
                try {
                    startNewGame();
                } catch (Exception e) {
                    Toast toast = Toast.makeText(AnagramsActivity.this, "Could not find a good starter word", Toast.LENGTH_LONG);
                    toast.show();
                }
        } else {
            getGiveUpConfirmationDialog().show();
        }
    }

    private AlertDialog.Builder getDifficultyDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            dictionary.increaseDifficulty();
                            startNewGame();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            startNewGame();
                            break;
                    }
                } catch (Exception e) {
                    dictionary.decreaseDifficulty();
                    Toast toast = Toast.makeText(AnagramsActivity.this, "Could not find a good starter word", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        };

        AlertDialog.Builder difficultyDialog = new AlertDialog.Builder(this);
        difficultyDialog.setMessage("Increase difficulty?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener);
        return difficultyDialog;
    }

    private AlertDialog.Builder getGiveUpConfirmationDialog() {
        AlertDialog.Builder confirmationDialog = new AlertDialog.Builder(this);
        confirmationDialog.setMessage("Are you sure you want to give up trying to find anagrams for " +
                "this word?\n\nYou will lose points for the words you haven't found.")
                .setPositiveButton("Yes", ((DialogInterface dialog, int which) -> showRemainingWords()))
                .setNegativeButton("No", null);
        return confirmationDialog;
    }

    private void startNewGame() throws Exception {
        final TextView gameStatus = (TextView) findViewById(R.id.gameStatusView);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final EditText editText = (EditText) findViewById(R.id.editText);
        final TextView resultView = (TextView) findViewById(R.id.resultView);

        currentWord = dictionary.pickGoodStarterWord();
        if (currentWord == null)
            throw new Exception("Starter word null");
        anagrams = dictionary.getAnagramsWithOneMoreLetter(currentWord);

        gameStatus.setText(Html.fromHtml(String.format(START_MESSAGE, currentWord.toUpperCase(), currentWord)));

        fab.setImageResource(android.R.drawable.ic_menu_help);
        fab.hide();

        resultView.setText("");

        editText.setText("");
        editText.setEnabled(true);
        editText.requestFocus();

        refreshRemaining();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void showRemainingWords() {
        final TextView gameStatus = (TextView) findViewById(R.id.gameStatusView);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final EditText editText = (EditText) findViewById(R.id.editText);
        final TextView resultView = (TextView) findViewById(R.id.resultView);

        editText.setText(currentWord);
        editText.setEnabled(false);

        fab.setImageResource(android.R.drawable.ic_media_play);

        currentWord = null;

        resultView.append(TextUtils.join("\n", anagrams));

        gameStatus.append(" Hit 'Play' to start again");

        int scoreDecrease = anagrams.stream()
                .mapToInt(String::length).sum();
        changeScoreTo(score - scoreDecrease);
    }
}
