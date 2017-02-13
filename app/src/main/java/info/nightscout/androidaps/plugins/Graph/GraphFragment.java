package info.nightscout.androidaps.plugins.Graph;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.nightscout.androidaps.R;
import info.nightscout.androidaps.interfaces.FragmentBase;

public class GraphFragment extends Fragment implements FragmentBase {

    static GraphPlugin graphPlugin;

    static public GraphPlugin getPlugin() {
        if (graphPlugin == null) {
            graphPlugin = new GraphPlugin();
        }
        return graphPlugin;
    }


    public GraphFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.graph_fragment, container, false);

        return view;
    }

}
