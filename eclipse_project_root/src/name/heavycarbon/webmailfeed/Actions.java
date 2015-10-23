package name.heavycarbon.webmailfeed;

import java.util.SortedSet;
import java.util.TreeSet;

/* 34567890123456789012345678901234567890123456789012345678901234567890123456789
 *******************************************************************************
 * Determine which actions to perform
 * 
 * All the actions is the "actionSet" will be executed by "main()"; commment
 * out any action that should not be performed!
 * 
 * 2015.08.23 - Moved out of main()
 ******************************************************************************/

public class Actions {

    public enum Action {
        addSubsetCommittee, addSubsetCeinturesNoires, addSubsetEnfants, addSubsetAdultes, addSubsetTousLesMembres, addSubsetEnfantsEtAdolescents
    }

    public static SortedSet<Action> ACTION_SET = new TreeSet<Action>();
        
    static {
        ACTION_SET.add(Action.addSubsetCommittee);
    	ACTION_SET.add(Action.addSubsetCeinturesNoires);
    	ACTION_SET.add(Action.addSubsetEnfants);
   	    ACTION_SET.add(Action.addSubsetEnfantsEtAdolescents);
    	ACTION_SET.add(Action.addSubsetAdultes);
    	ACTION_SET.add(Action.addSubsetTousLesMembres);
    }
    
    private Actions() {
    	// cannot be instantiated
    }
}
