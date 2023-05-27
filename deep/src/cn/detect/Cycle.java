package cn.njust.cy.detect;

import java.util.ArrayList;


import java.util.Collection;
import java.util.HashSet;

import cn.njust.cy.entity.Dependency;
import cn.njust.cy.entity.DependencyDetail;
import depends.Depends;
import depends.matrix.core.DependencyMatrix;
import depends.matrix.core.DependencyPair;
import depends.matrix.core.DependencyValue;

public class Cycle {
	private DependencyMatrix matrix;
	private Collection<DependencyPair> dependencyPairs;
	public Cycle(String inputDir) {
		this.matrix = new Depends().getMatrix(inputDir);
		this.dependencyPairs = matrix.getDependencyPairs();
	}
	public Collection<Dependency> getCycle() {
		Graph graph = new Graph(matrix.getNodes().size());
		for(DependencyPair pair:dependencyPairs) {
			graph.addEdge(pair.getFrom(), pair.getTo());
		}
		graph.printSCCs();
		ArrayList<ArrayList<Integer>> ansList = graph.getList();
		Collection<Dependency> cycleDep = new HashSet<Dependency>();
		int count =  0;
		for(ArrayList<Integer> ans:ansList) {
			if(ans.size()==2) {
				System.out.println(ans.get(0)+" "+ans.get(1));
				DependencyPair depA = findPairById(ans.get(0),ans.get(1)); 
				DependencyPair depB = findPairById(ans.get(1),ans.get(0)); 
				if(depA!=null&&depB!=null) {
					String classA = matrix.getNodeName(depA.getFrom());
					String classB = matrix.getNodeName(depB.getFrom());
					DependencyDetail[] detail = new  DependencyDetail[2];
					detail[0] = new DependencyDetail(count,"AtoB",classA,depA.getDependencies());
					detail[1] = new DependencyDetail(count,"BtoA",classB,depB.getDependencies());
					Dependency dependency = new Dependency(count,detail);
					count++;
					cycleDep.add(dependency);
				}
				
			}
        }
		return cycleDep;
	}
	
	public DependencyPair findPairById(int from,int to) {
		for(DependencyPair pair:dependencyPairs) {
			if(pair.getFrom().equals(from)&&pair.getTo().equals(to)) {
				return pair;
//				System.out.println(pair.getFromName()+pair.getToName());
			}
		}
		return null;
	}
	public HashSet<String> getImplementsRelation(String interfaceName) {
		HashSet<String> froms = new HashSet<String>();
		for(DependencyPair pair:dependencyPairs) {
//			System.out.println(pair.getFrom()+" "+pair.getTo()+" "+pair.getFromName()+" "+pair.getToName());
			for(DependencyValue value:pair.getDependencies()) {
				if(value.getType().equals("Implement")) {
					if(value.getDetailTo().equals(interfaceName)) {
						froms.add(value.getDetailFrom());
					}
				}
			}
		}
		return froms;
	}
	
	public HashSet<String> getCallers(String str){
		HashSet<String> callers = new HashSet<String>();
		for(DependencyPair pair:dependencyPairs) {
			for(DependencyValue value:pair.getDependencies()) {
				if(value.getType().equals("Call")&&value.getDetailTo().equals(str)) {
					callers.add(value.getDetailFrom());
				}
			}
		}
		return callers;
	}
	public static void main(String[] args) {
		Cycle relation = new Cycle("E:/test/cassandra-trunk");
		Collection<Dependency> res = relation.getCycle();
		for(Dependency detail:res) {
			System.out.println(detail.getDetail()[0].getName()+" "+detail.getDetail()[1].getName());
			for(DependencyValue val:detail.getDetail()[0].getValues()) {
				System.out.println("depA---"+val.getType()+" "+val.getDetailFrom()+" "+val.getDetailTo());
			}
			for(DependencyValue val:detail.getDetail()[1].getValues()) {
				System.out.println("depB---"+val.getType()+" "+val.getDetailFrom()+" "+val.getDetailTo());
			}
		}
//		System.out.println("--------------------------------------------");
//		relation.getImplementsRelation("CH.ifa.draw.util.PaletteListener");
	}
}
