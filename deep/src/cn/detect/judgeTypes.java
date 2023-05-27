package cn.njust.cy.detect;

import java.util.Collection;

import cn.njust.cy.entity.Dependency;
import depends.matrix.core.DependencyValue;

public class judgeTypes {
	private Dependency dependency;
	public judgeTypes(Dependency dependency){
		this.dependency = dependency;
	}
	public int getTypes() {
		Collection<DependencyValue> valueList1 = dependency.getDetail()[0].getValues();
		Collection<DependencyValue> valueList2 = dependency.getDetail()[1].getValues();
		int state = -1;
		for(DependencyValue value:valueList1) {			
			if(value.getType().equals("Create")) {//提取A
				state = 1;
			}
			if(value.getType().equals("Implements")||value.getType().equals("Extend")) {
				return 3;
			}
			if(valueList1.size()==1&&value.getType().equals("Import")) return 5;
		}
		for(DependencyValue value:valueList2) {
			if(value.getType().equals("Create")) {//提取B
				state = 2;
			}
			if(value.getType().equals("Implements")||value.getType().equals("Extend")) {
				return 4;
			}
			if(valueList1.size()==1&&value.getType().equals("Import")) return 5;
		}
		
		return state;
	}
}
