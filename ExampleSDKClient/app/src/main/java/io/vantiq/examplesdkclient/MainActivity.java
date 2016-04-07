package io.vantiq.examplesdkclient;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.vantiq.client.BaseResponseHandler;
import io.vantiq.client.SortSpec;
import io.vantiq.client.Vantiq;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private SelectTypesTask selectTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Add "done" button handler which navigates back to the login page
        Button doneButton = (Button) findViewById(R.id.done_button);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Handle list view
        this.listView = (ListView) findViewById(R.id.listView);

        // Trigger select query
        this.selectTask = new SelectTypesTask(((ExampleApplication) getApplication()).getVantiq());
        this.selectTask.execute((Void) null);
    }

    /**
     * When the async task has completed, the result is rendered in the list
     */
    public void onServerCompletion(List<Map<String,String>> results) {
        SimpleAdapter adapter = new SimpleAdapter(this,
                                                  results,
                                                  R.layout.list_fragment,
                                                  new String[] { "name", "namespace" },
                                                  new int[] { R.id.list_name, R.id.list_ns});
        this.listView.setAdapter(adapter);
    }

    /**
     * When an error occurs
     */
    public void onServerError(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        builder.show();
    }

    /**
     * Async task to perform a select to return all available types from the Vantiq server.
     */
    public class SelectTypesTask extends VantiqAsyncTask<Void, Void, List<Map<String,String>>> {

        SelectTypesTask(Vantiq vantiq) {
            super(vantiq);
        }

        @Override
        protected void doRequest(Vantiq vantiq, BaseResponseHandler handler) {
            vantiq.select(Vantiq.SystemResources.TYPES.value(),
                          Arrays.asList("name", "namespace"),
                          null,
                          new SortSpec("name", /*descending=*/ false),
                          handler);
        }

        @Override
        protected List<Map<String,String>> prepareResult(BaseResponseHandler handler) {
            List<Map<String,String>> result = Lists.newArrayList();

            // At this point, we should have a result, so we pull out the response
            if(handler.getBody() != null) {
                for(JsonObject obj : handler.getBodyAsList()) {
                    Map<String,String> entry = Maps.newHashMap();
                    entry.put("name", obj.get("name").getAsString());
                    entry.put("namespace", obj.get("namespace").getAsString());
                    result.add(entry);
                }
            } else if(handler.hasErrors()) {
                MainActivity.this.onServerError(handler.getErrors().toString());
            } else if(handler.hasException()) {
                MainActivity.this.onServerError(handler.getException().getMessage());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final List<Map<String,String>> result) {
            MainActivity.this.selectTask = null;
            onServerCompletion(result);
        }
    }

}
