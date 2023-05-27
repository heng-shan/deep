package cn.njust.cy.actions;
import java.io.File;



import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import Designite.SourceModel.*;
import Designite.utils.Constants;
import Designite.utils.Logger;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.lang.reflect.InvocationTargetException;

import java.util.*;
import java.util.List;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditor;

import Designite.InputArgs;
import Designite.SourceModel.SM_Project;
import Designite.utils.Constants;
import Designite.utils.Logger;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;

import cn.njust.cy.detect.Cycle;
import cn.njust.cy.detect.judgeTypes;
import cn.njust.cy.entity.Dependency;
import cn.njust.cy.entity.DependencyDetail;
import cn.njust.cy.actions.InversionRefactoring;
import cn.njust.cy.actions.MoveRefactoring;
import cn.njust.cy.actions.RemoveImport;
import depends.matrix.core.DependencyValue;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
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

import Designite.InputArgs;
import Designite.SourceModel.SM_Project;
import Designite.utils.Constants;
import Designite.utils.Logger;
import cn.njust.cy.entity.DependencyDetail;
import depends.matrix.core.DependencyValue;











public class MoveRefactoring extends Refactoring{
	private IFile fileA;
	private IFile fileB;
	private String newFileName;
	private DependencyDetail detail;
	private HashSet<MethodDeclaration> methodsToRemove ;
	private HashSet<String> changeFiles;
	public MoveRefactoring(IFile fileA,IFile fileB,String newFileName,DependencyDetail detail,HashSet<String> changeFiles) {
		this.fileA = fileA;
		this.fileB = fileB;
		this.newFileName = "Another" + fileA.getName().replace(".java", "");
		this.detail = detail;
		this.methodsToRemove = new HashSet<MethodDeclaration>();
		this.changeFiles = changeFiles;
		removeOldMethod();
		createNewClass();
		modifyChanges();
		//IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
	}
	
	
	static void MoveFunction(SM_Project project) throws FileNotFoundException
	{
		
		project.parse();

		//CompilationUnit sourceUnit=project.getCompilationUnitList().get(1);
		CompilationUnit sourceUnit=project.getCompilationUnitList().get(0);
		System.out.print("CompilationUnit:     "+sourceUnit+"\n");
		TypeDeclaration sourceTypeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		
		TypeDeclaration sourceTypeDec2 = (TypeDeclaration) sourceUnit.types().get(1);
//		sourceTypeDec2.delete();
		
		System.out.print("TypeDeclaration:     "+sourceTypeDec2.getName()+"\n");
		MethodDeclaration[] sourceMethodDecls = sourceTypeDecl.getMethods();
		System.out.print("sourceMethod:     "+sourceMethodDecls[0].getName()+"\n");
		AST ast = sourceUnit.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		//AST ast1 = sourceMethodDecls.getAST();
		System.out.print(ast+"\n");
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);	
        MethodDeclaration methodDecl = sourceTypeDecl.getMethods()[0];
        Block block = methodDecl.getBody();
        
        System.out.print("block:   "+block+"\n");
        
        
        TypeDeclaration programClass = ast.newTypeDeclaration();
        programClass.setInterface(false);
        //programClass.bodyDeclarations().add(helloMethod);
        programClass.setName(ast.newSimpleName(sourceTypeDec2.getName().toString()));
        // 设定类或接口的修饰类型public
        //programClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
       // programClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.));
        // 将创建好的类添加到文件
        sourceUnit.types().add(programClass);
        
        
        //sourceTypeDecl.setName(ast.newSimpleName("MyController"));
        MethodDeclaration helloMethod= ast.newMethodDeclaration();
        
        helloMethod.setName(ast.newSimpleName(sourceMethodDecls[0].getName().toString()));
        helloMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        helloMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
        
        // 为方法增加语句块
        Block helloBlock = ast.newBlock();
       
       // System.out.print("block:   "+helloBlock+"\n");
        //helloBlock.setProperty(null, helloBlock);
        
        
//        ASTParser parser = ASTParser.newParser(AST.JLS8);
//        parser.setSource("System.out.println(\"Hello\" + \" world\");".toCharArray());
//        parser.setKind(ASTParser.K_STATEMENTS);
//        Block block2 = (Block) parser.createAST(null);
        Block block2 = ast.newBlock();
        // (3) copy the statements to the existing AST
        block2 = (Block) ASTNode.copySubtree(ast, block);
        helloMethod.setBody(block2);
        
        // 将方法装入类中
        //sourceTypeDecl.bodyDeclarations().add(helloMethod);
        
        
        sourceTypeDecl.setName(ast.newSimpleName("qqgg"));
        sourceTypeDec2.setName(ast.newSimpleName("ffff"));
        
        sourceTypeDecl.delete();
        sourceTypeDec2.delete();
        programClass.bodyDeclarations().add(helloMethod);
        


        // 最后打印出创建的代码内容
        System.out.println(sourceUnit.toString());

        String path="E:\\eclipse_work\\test\\src\\test\\hello\\test.java";
        PrintStream stream=null;
        stream=new PrintStream(path);
        stream.print(sourceUnit.toString());
	}

	
	static void Hide(SM_Project project) throws FileNotFoundException
	{
		project.parse();

		//CompilationUnit sourceUnit=project.getCompilationUnitList().get(1);
		CompilationUnit sourceUnit=project.getCompilationUnitList().get(0);
		System.out.print("CompilationUnit:     "+sourceUnit+"\n");
		TypeDeclaration sourceTypeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		
		TypeDeclaration sourceTypeDec2 = (TypeDeclaration) sourceUnit.types().get(1);
//		sourceTypeDec2.delete();
		
		System.out.print("TypeDeclaration:     "+sourceTypeDec2.getName()+"\n");
		MethodDeclaration[] sourceMethodDecls = sourceTypeDecl.getMethods();
		System.out.print("sourceMethod:     "+sourceMethodDecls[0].getName()+"\n");
		AST ast = sourceUnit.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		//AST ast1 = sourceMethodDecls.getAST();
		System.out.print(ast+"\n");
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);	
        MethodDeclaration methodDecl = sourceTypeDecl.getMethods()[0];
        Block block = methodDecl.getBody();
        
        System.out.print("block:   "+block+"\n");
        
        
        TypeDeclaration programClass = ast.newTypeDeclaration();
        programClass.setInterface(false);
        //programClass.bodyDeclarations().add(helloMethod);
        programClass.setName(ast.newSimpleName(sourceTypeDec2.getName().toString()));
        // 设定类或接口的修饰类型public
        //programClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
       // programClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.));
        // 将创建好的类添加到文件
        sourceUnit.types().add(programClass);
        
        
        //sourceTypeDecl.setName(ast.newSimpleName("MyController"));
        MethodDeclaration helloMethod= ast.newMethodDeclaration();
        
        helloMethod.setName(ast.newSimpleName(sourceMethodDecls[0].getName().toString()));
        helloMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        helloMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
        
        // 为方法增加语句块
        Block helloBlock = ast.newBlock();
       
       // System.out.print("block:   "+helloBlock+"\n");
        //helloBlock.setProperty(null, helloBlock);
        
        
//        ASTParser parser = ASTParser.newParser(AST.JLS8);
//        parser.setSource("System.out.println(\"Hello\" + \" world\");".toCharArray());
//        parser.setKind(ASTParser.K_STATEMENTS);
//        Block block2 = (Block) parser.createAST(null);
        Block block2 = ast.newBlock();
        // (3) copy the statements to the existing AST
        block2 = (Block) ASTNode.copySubtree(ast, block);
        helloMethod.setBody(block2);
        
        // 将方法装入类中
        //sourceTypeDecl.bodyDeclarations().add(helloMethod);
        
        
        sourceTypeDecl.setName(ast.newSimpleName("qqgg"));
        sourceTypeDec2.setName(ast.newSimpleName("ffff"));
        
        sourceTypeDecl.delete();
        sourceTypeDec2.delete();
        programClass.bodyDeclarations().add(helloMethod);
        


        // 最后打印出创建的代码内容
        System.out.println(sourceUnit.toString());

        String path="E:\\eclipse_work\\test\\src\\test\\hello\\test.java";
        PrintStream stream=null;
        stream=new PrintStream(path);
        stream.print(sourceUnit.toString());
	}
	
	
	static void test(SM_Project project) throws FileNotFoundException
	{
		project.parse();

		//CompilationUnit sourceUnit=project.getCompilationUnitList().get(1);
		CompilationUnit sourceUnit=project.getCompilationUnitList().get(0);
		System.out.print("CompilationUnit:     "+sourceUnit+"\n");
		TypeDeclaration sourceTypeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		
		TypeDeclaration sourceTypeDec2 = (TypeDeclaration) sourceUnit.types().get(1);
//		sourceTypeDec2.delete();
		
		System.out.print("TypeDeclaration:     "+sourceTypeDec2.getName()+"\n");
		MethodDeclaration[] sourceMethodDecls = sourceTypeDecl.getMethods();
		System.out.print("sourceMethod:     "+sourceMethodDecls[0].getName()+"\n");
		AST ast = sourceUnit.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		//AST ast1 = sourceMethodDecls.getAST();
		System.out.print(ast+"\n");
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);	
        MethodDeclaration methodDecl = sourceTypeDecl.getMethods()[0];
        Block block = methodDecl.getBody();
        
        System.out.print("block:   "+block+"\n");
        
        
        
        
        Block new_block=ast.newBlock();
        
        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setName(ast.newSimpleName("department"));
        VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
        statement.setType(ast.newSimpleType(ast.newSimpleName(sourceTypeDecl.getName().toString())));
        
        ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
        classInstanceCreation.setType(ast.newSimpleType(ast.newSimpleName(sourceTypeDecl.getName().toString())));
        
        fragment.setInitializer(classInstanceCreation);
        
        new_block.statements().add(statement);
        
        
        MethodInvocation methodInvocation2 = ast.newMethodInvocation();
        methodInvocation2.setExpression(ast.newName(fragment.getName().toString()));
        methodInvocation2.setName(ast.newSimpleName(methodDecl.getName().toString()));
   
        ExpressionStatement statement2 = ast.newExpressionStatement(methodInvocation2);
        
        new_block.statements().add(statement2);
//        MethodInvocation methodInvocation = ast.newMethodInvocation();
//        methodInvocation.setExpression(ast.newSimpleName("program"));
//        methodInvocation.setName(ast.newSimpleName("getString"));
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        TypeDeclaration programClass = ast.newTypeDeclaration();
        programClass.setInterface(false);
        //programClass.bodyDeclarations().add(helloMethod);
        programClass.setName(ast.newSimpleName(sourceTypeDec2.getName().toString()));

        //sourceUnit.types().add(programClass);
        
        
        //sourceTypeDecl.setName(ast.newSimpleName("MyController"));
        MethodDeclaration helloMethod= ast.newMethodDeclaration();
        
        helloMethod.setName(ast.newSimpleName(sourceMethodDecls[0].getName().toString()));
        helloMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        helloMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
        
        // 为方法增加语句块
        Block helloBlock = ast.newBlock();

        Block block2 = ast.newBlock();
        // (3) copy the statements to the existing AST
        block2 = (Block) ASTNode.copySubtree(ast, new_block);
        helloMethod.setBody(block2);
        //System.out.println("测试:    "+sourceTypeDec2.getName().toString())+"\n");
        sourceTypeDec2.setSuperclassType(null);
        
        //sourceTypeDecl.delete();
        //sourceTypeDec2.delete();
        sourceTypeDec2.bodyDeclarations().add(helloMethod);
       // programClass.bodyDeclarations().add(helloMethod);
        


        // 最后打印出创建的代码内容
        System.out.println(sourceUnit.toString());

//        String path="E:\\eclipse_work\\test\\src\\test\\hello\\test.java";
//        PrintStream stream=null;
//        stream=new PrintStream(path);
//        stream.print(sourceUnit.toString());
	}
	
	public static void main(String[] args) throws IOException, MalformedTreeException, BadLocationException, CoreException {
	
		System.out.print("start refactoring"+"\n");
		InputArgs argsObj=new InputArgs("E:\\eclipse_work\\test", "E:\\test\\out");
		SM_Project project = new SM_Project(argsObj);

		
		//MoveFunction(project);
        test(project);
        
        
        
        
		
//		System.out.print("project:   "+project.getName()+"\n");
		project.resolve();
		//System.out.print(project.getHierarchyGraph().);
		project.computeMetrics();
		//System.out.print("hierar:   "+project.getHierarchyGraph().getConnectedComponnents().get(0).get(0)+"\n");
		project.detectCodeSmells();
//		if (Constants.DEBUG)
//			writeDebugLog(argsObj, project);
		Logger.log("Done.");
		
	}
	
	
	
	public void removeOldMethod() {
		IFolder folder = (IFolder) fileA.getParent();
		IPackageFragment mypackage = (IPackageFragment)JavaCore.create(folder);
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnit = parse(sourceIUnit);	
		TypeDeclaration sourceTypeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		MethodDeclaration[] sourceMethodDecls = sourceTypeDecl.getMethods();
		AST ast = sourceUnit.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);	
		ListRewrite bodyListRewrite = sourceRewriter.getListRewrite(sourceTypeDecl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		HashSet<String> methodsToRemoveName = new HashSet<String>();
		for(DependencyValue val:detail.getValues()) {
			System.out.println(val.getDetailFrom());
			String []splits = val.getDetailFrom().split("\\.");
			methodsToRemoveName.add(splits[splits.length-1]);
		}
		for(MethodDeclaration methodDecl:sourceMethodDecls) {
			for(String methodName:methodsToRemoveName) {
				if(methodDecl.getName().toString().equals(methodName)) {
					methodsToRemove.add(methodDecl);
					bodyListRewrite.remove(methodDecl, null);
				}
			}
		}
		try {
			Document document = new Document(sourceIUnit.getSource());
			TextEdit edits = sourceRewriter.rewriteAST();
			edits.apply(document);
			sourceIUnit.getBuffer().setContents(document.get());
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
	
	public void createNewClass() {
		IFolder sourceFolder = (IFolder) fileA.getParent();
		IPackageFragment sourcePackage = (IPackageFragment)JavaCore.create(sourceFolder);
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnit = parse(sourceIUnit);
		try {
			ICompilationUnit extractIUnit = sourcePackage.createCompilationUnit(newFileName+".java","",false, null);
			if(sourceUnit.getPackage() != null) {
				extractIUnit.createPackageDeclaration(sourceUnit.getPackage().getName().toString(), null);
			}
			String typeStr = "public class " + newFileName + " {"+ "\n" + "}";
			extractIUnit.createType(typeStr,null,true, null);
			IType type = extractIUnit.getType(newFileName);
			
			Set<String> requiredImportClass = new HashSet<String>(); //需要import的class
			for(MethodDeclaration methodDecl:methodsToRemove) {
				type.createMethod(methodDecl.toString(), null, true, null);
				methodDecl.accept(new ASTVisitor() {
					public boolean visit(SimpleType node) {
						System.out.println("name "+node.resolveBinding().getBinaryName());
						requiredImportClass.add(node.resolveBinding().getBinaryName());
						return true;
					}
				});
			}
			for(String importStr:requiredImportClass) {
				String requiredImportpackage = importStr.substring(0, importStr.lastIndexOf("."));
				if(!requiredImportpackage.equals("java.lang")&&!requiredImportpackage.equals(sourcePackage.getElementName()))
					extractIUnit.createImport(importStr,null, null);
			}
			IMethod[] methods = type.getMethods();		 
			System.out.println("methods "+methods.length);
			for (IMethod method : methods) {
				HashSet<IMethod> callers = getCallersOf(method);
				System.out.println("callers: "+callers.size());
				for(IMethod c:callers) {
					System.out.println("callers: "+c.getElementName());
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void modifyChanges() {
		if(changeFiles!=null) { 
			String []arr = new String[2];
			for(String str:changeFiles) {
				arr[0] = str.substring(0,str.lastIndexOf("."));
				arr[1] = str.substring(str.lastIndexOf(".")+1);
				System.out.println("class  "+arr[0]);
				System.out.println("method  "+arr[1]);
				modifyCallChange(arr);
			}
		}
	}
	
	public void modifyCallChange(String[] arr) {
		IJavaProject myProject = ((ICompilationUnit)JavaCore.create(fileA)).getJavaProject();//获取当前项目
		try {
			IType type = myProject.findType(arr[0]);
			ICompilationUnit IUnit = type.getCompilationUnit();
			CompilationUnit unit = parse(IUnit);
			TypeDeclaration typeDecl = (TypeDeclaration) unit.types().get(0);
			MethodDeclaration[] methodDecls = typeDecl.getMethods();
			MethodDeclaration methodDecl = findMethodByName(methodDecls,arr[1]);
			AST ast = unit.getAST();
			ASTRewrite sourceRewriter = ASTRewrite.create(ast);
			String sourceName = fileA.getName().replaceAll(".java", "");
			System.out.println("methodDecl  "+ methodDecl.getName());
			
			methodDecl.accept(new ASTVisitor() {
				public boolean visit(SimpleName node) {
					System.out.println("node : "+node);
					if(node.toString().equals(sourceName)) {
						sourceRewriter.set(node,SimpleType.NAME_PROPERTY, ast.newName(newFileName), null);	
//						sourceRewriter.set(node,SimpleName.VAR_PROPERTY, ast.newName(newFileName), null);	
					}
					return true;
				}
			});
			try {
				Document document = new Document(IUnit.getSource());
				TextEdit edits = sourceRewriter.rewriteAST();
				edits.apply(document);
				IUnit.getBuffer().setContents(document.get());
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
			
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public String getNewFileName() {
		return newFileName;
	}

	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
	
	 
	public HashSet<IMethod> getCallersOf(IMethod m) {
		CallHierarchy callHierarchy = CallHierarchy.getDefault();
		IMember[] members = {m};
		MethodWrapper[] methodWrappers = callHierarchy.getCallerRoots(members);
		HashSet<IMethod> callers = new HashSet<IMethod>();
		for (MethodWrapper mw : methodWrappers) {
			MethodWrapper[] mw2 = mw.getCalls(new NullProgressMonitor());
			HashSet<IMethod> temp = getIMethods(mw2);
			callers.addAll(temp);    
		}
		return callers;
	}

	public HashSet<IMethod> getIMethods(MethodWrapper[] methodWrappers) {
		HashSet<IMethod> c = new HashSet<IMethod>(); 
		for (MethodWrapper m : methodWrappers) {
			IMethod im = getIMethodFromMethodWrapper(m);
			if (im != null) {
				c.add(im);
			}
		}
		return c;
	}

	public IMethod getIMethodFromMethodWrapper(MethodWrapper m) {
		try {
			IMember im = m.getMember();
			if (im.getElementType() == IJavaElement.METHOD) {
				return (IMethod)m.getMember();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public MethodDeclaration findMethodByName(MethodDeclaration[] methodDecls,String methodName) {
//		public Set<MethodDeclaration> findMethodByName(MethodDeclaration[] methodDecls,String methodName) {
			Set <MethodDeclaration> candidateMethodDecls = new HashSet<MethodDeclaration>();
			for(MethodDeclaration methodDecl:methodDecls) {
				if(methodDecl.getName().toString().equals(methodName)) {
					candidateMethodDecls.add(methodDecl);
					return methodDecl;
				}
			}
//			return candidateMethodDecls;
			return null;
		}
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			removeOldMethod();
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
				try {
					pm.beginTask("Creating change...", 1);
					final Collection<Change> changes = new ArrayList<Change>();
//					changes.addAll(compilationUnitChanges.values());
//					changes.addAll(createCompilationUnitChanges.values());
					CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
						@Override
						public ChangeDescriptor getDescriptor() {
							ICompilationUnit IUnit = (ICompilationUnit)JavaCore.create(fileA);
							CompilationUnit unit = parse(IUnit);
							TypeDeclaration sourceTypeDecl = (TypeDeclaration) unit.types().get(0);
							String projectName = IUnit.getJavaProject().getElementName();
							String description = MessageFormat.format("Refactor from ''{0}''", new Object[] { sourceTypeDecl.getName().getIdentifier()});
							String comment = null;
							return new RefactoringChangeDescriptor(new MoveRefactoringDescriptor(projectName,description,
									comment,fileA,fileB,newFileName,detail,changeFiles));
						}
					};
					return change;
				} finally {
					pm.done();
				}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "MoveRefactoring";
	}
}
