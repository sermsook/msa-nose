package edu.baylor.ecs.msanose.model.context;

import edu.baylor.ecs.msanose.model.Pair;
import edu.baylor.ecs.msanose.model.UnorderedPair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor @AllArgsConstructor
@Data public class CyclicDependencyContext {
    List<DependencyContext> cycles;
    double ratioOfCyclicDependency;
    double totalServiceCall;
    double totalCyclicCall;

    ArrayList<Pair> pairs = new ArrayList<>();

    public ArrayList<Pair> addPair(Pair pair){
        this.pairs.add(pair);
        return this.pairs;
    }
}
