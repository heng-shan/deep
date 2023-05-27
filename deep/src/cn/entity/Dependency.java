package cn.njust.cy.entity;

public class Dependency {
	private int id;
	private DependencyDetail[] detail;

	public Dependency(int id,DependencyDetail[] detail) {
		this.id = id;
		this.detail = detail;
	}

	public DependencyDetail[] getDetail() {
		return detail;
	}

	public void setDetail(DependencyDetail[] detail) {
		this.detail = detail;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	
	
}
