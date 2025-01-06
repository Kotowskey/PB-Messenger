package projekt.pb.sm.Fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import projekt.pb.sm.databinding.FragmentCallsBinding;

public class CallsFragment extends Fragment {

    private FragmentCallsBinding binding;

    public CallsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCallsBinding.inflate(inflater, container, false);

        binding.callsMessage.setText("Calls feature coming soon!");
        binding.callsDescription.setText("This feature will allow you to make voice and video calls.");

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}