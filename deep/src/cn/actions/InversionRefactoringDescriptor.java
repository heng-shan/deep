package cn.njust.cy.actions;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import cn.njust.cy.entity.DependencyDetail;

public class InversionRefactoringDescriptor  extends RefactoringDescriptor {

	public static final String REFACTORING_ID = "org.eclipse.inversion";
	private IFile fileA;
	private IFile fileB;
	private String newFileName;
	private DependencyDetail details;
	private Collection<String> changes;
	
	protected InversionRefactoringDescriptor(String projectName, String description, String comment,
			IFile fileA,IFile fileB,String newFileName,DependencyDetail details,Collection<String> changes) {
		super(REFACTORING_ID, projectName, description, comment,  RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		// TODO Auto-generated constructor stub
		this.fileA = fileA;
		this.fileB = fileB;
		this.newFileName = newFileName;
		this.details = details;
		this.changes = changes;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status) throws CoreException {
		// TODO Auto-generated method stub
		Refactoring refactoring = new InversionRefactoring(fileA,fileB,newFileName,details,changes);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
//		return null;
	}

}
