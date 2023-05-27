package cn.njust.cy.actions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class RemoveImport extends Refactoring{
	private IFile fileA;
	private IFile fileB;
	public RemoveImport(IFile fileA,IFile fileB) {
		this.fileA = fileA;
		this.fileB = fileB;
		apply();
	}
	public void apply() {
		System.out.println("apply");
		ICompilationUnit sourceIUnitA = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnitA = parse(sourceIUnitA);	
		CompilationUnit sourceUnitB = parse((ICompilationUnit)JavaCore.create(fileB));		
		IPackageFragment sourcePackageB = (IPackageFragment)JavaCore.create((IFolder) fileB.getParent());
		AST ast = sourceUnitA.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);	
		List<ImportDeclaration> importDecls = sourceUnitA.imports();
		ListRewrite packageRewrite = sourceRewriter.getListRewrite(sourceUnitA, CompilationUnit.IMPORTS_PROPERTY);
		for(ImportDeclaration importDecl:importDecls) {
			String strB = sourcePackageB.getElementName()+ "."+fileB.getName().replace(".java", "");
			if(strB.equals(importDecl.getName().toString())) {
				System.out.println(importDecl);
				packageRewrite.remove(importDecl, null);
			}
		}
		try {
			Document document = new Document(sourceIUnitA.getSource());
			TextEdit edits = sourceRewriter.rewriteAST();
			edits.apply(document);
			sourceIUnitA.getBuffer().setContents(document.get());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			apply();
		} finally {
			pm.done();
		}
		return status;
	}
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
		} finally {
			pm.done();
		}
		return status;
	}
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
//		try {
//			pm.beginTask("Creating change...", 1);
//			final Collection<Change> changes = new ArrayList<Change>();
//			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
//				@Override
//				public ChangeDescriptor getDescriptor() {
//					return null;
//				}
//			};
//			return change;
//		}finally {
//			pm.done();
//		}
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "RemoveImport";
	}
	

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}
