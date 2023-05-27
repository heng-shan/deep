package cn.njust.cy.actions;

import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import cn.njust.cy.entity.DependencyDetail;

public class MoveRefactoringDescriptor  extends RefactoringDescriptor{

	public static final String REFACTORING_ID = "org.eclipse.move";
	private IFile fileA;
	private IFile fileB;
	private String newFileName;
	private DependencyDetail details;
	private HashSet<String> changeFiles;
	
	protected MoveRefactoringDescriptor(String projectName, String description, String comment,
			IFile fileA,IFile fileB,String newFileName,DependencyDetail details,HashSet<String> changeFiles) {
		super(REFACTORING_ID, projectName, description, comment,  RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		// TODO Auto-generated constructor stub
		this.fileA = fileA;
		this.fileB = fileB;
		this.newFileName = newFileName;
		this.details = details;
		this.changeFiles = changeFiles;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status) throws CoreException {
		// TODO Auto-generated method stub
		Refactoring refactoring = new MoveRefactoring(fileA,fileB,newFileName,details,changeFiles);
		RefactoringStatus refStatus = new RefactoringStatus();
		status.merge(refStatus);
		return refactoring;
	}

}
