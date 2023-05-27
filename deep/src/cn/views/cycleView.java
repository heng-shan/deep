package cn.njust.cy.views;


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
import java.text.SimpleDateFormat;
import java.lang.reflect.InvocationTargetException;

import java.util.*;
import java.util.List;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
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

public class cycleView extends ViewPart {
	private static final String MESSAGE_DIALOG_TITLE = "MyViewer";
	
	private TreeViewer treeViewer;
	
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
    Dependency[] dependencies;
    private IJavaProject selectedProject;
	private IJavaProject activeProject;
	private Set<IJavaElement> javaElementsToOpenInEditor;
	private Cycle cycle;

	class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(dependencies!=null) {
				return dependencies;
			}
			else {
				return new Dependency[] {};
			}
		}
		public Object[] getChildren(Object parentElement) {
          if(parentElement instanceof Dependency) {
        	return ((Dependency)parentElement).getDetail();
          }else if(parentElement instanceof DependencyDetail){
        	return ((DependencyDetail)parentElement).getValues().toArray();
          }else {
        	return new DependencyValue[] {};
          }
		}
		public Object getParent(Object element) {
        	return null;
		}
		public boolean hasChildren(Object element) {
			 return getChildren(element).length > 0;
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object element, int index) {
        	if(element instanceof Dependency) {
        		Dependency entry = (Dependency)element;
        		switch (index) {
        		case 1:
        			return entry.getDetail()[0].getName();
        		case 2:
        			return entry.getDetail()[1].getName();
        		case 3:
        			return entry.getDetail()[0].getValues().size()+"/"+entry.getDetail()[1].getValues().size();
        		default:
        			return "";
        		}
        	} 
        	else if(element instanceof DependencyDetail) {
        		DependencyDetail entry = (DependencyDetail)element;
        		switch (index) {
        		case 0:
        			return entry.getStr();
//        		case 1:{
//        			return entry.getName();
//        		}
        		default:
        			return "";
        		}
        	}
        	else if(element instanceof DependencyValue) {
        		DependencyValue entry = (DependencyValue)element;
        		switch (index) {
        		case 0:
        			return "["+entry.getType()+"]";
        		case 1:
        			return entry.getDetailFrom();
        		case 2:
        			return entry.getDetailTo();
        		default:
        			return "";
        		}
        	} 
        	else return "";
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	class NameSorter extends ViewerSorter {
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			return ((Dependency)obj1).getId()-((Dependency)obj2).getId();
		}
	}
	/**
	 * The constructor.
	 */
	public cycleView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		treeViewer.setContentProvider(new ViewContentProvider());
		treeViewer.setLabelProvider(new ViewLabelProvider());
//		treeViewer.setSorter(new NameSorter());
		treeViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(20, true));
//		layout.addColumnData(new ColumnWeightData(50, true));
//		layout.addColumnData(new ColumnWeightData(50, true));
		layout.addColumnData(new ColumnWeightData(10, true));
		treeViewer.getTree().setLayout(layout);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		new TreeColumn(treeViewer.getTree(), SWT.LEFT).setText("Type");
//		new TreeColumn(treeViewer.getTree(), SWT.LEFT).setText("ClassA");
//		new TreeColumn(treeViewer.getTree(), SWT.LEFT).setText("ClassB");
		new TreeColumn(treeViewer.getTree(), SWT.LEFT).setText("nums");
		treeViewer.expandAll();

		for (int i = 0, n = treeViewer.getTree().getColumnCount(); i < n; i++) {
			treeViewer.getTree().getColumn(i).pack();
		}

		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
//		JavaCore.addElementChangedListener(ElementChangedListener.getInstance());
		getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(new IOperationHistoryListener() {
			public void historyNotification(OperationHistoryEvent event) {
				int eventType = event.getEventType();
				if(eventType == OperationHistoryEvent.UNDONE  || eventType == OperationHistoryEvent.REDONE ||
						eventType == OperationHistoryEvent.OPERATION_ADDED || eventType == OperationHistoryEvent.OPERATION_REMOVED) {
					if(activeProject != null) {
//						applyRefactoringAction.setEnabled(false);
					}
				}
			}
		});
	}
	


	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(applyRefactoringAction);
	}
	
	
//	public void main(String[] args) {
//		InputArgs argsObj=new InputArgs("E:\\test\\cassandra-trunk", "E:\\test\\out");
//		SM_Project project = new SM_Project(argsObj);
//		//Logger.logFile = getlogFileName(argsObj);
//		//TODO: log the version number
//		project.parse();
//		project.resolve();
//		project.computeMetrics();
//		project.detectCodeSmells();
////		if (Constants.DEBUG)
////			writeDebugLog(argsObj, project);
//		Logger.log("Done.");	
//	}
//	
	
	
	
//	private void AddComments() throws MalformedTreeException, BadLocationException, CoreException {
//
//	    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("test");
//	    IJavaProject javaProject = JavaCore.create(project);
//	    IPackageFragment package1 = javaProject.getPackageFragments()[0];
//
//	    //get first compilation unit
//
//	    // parse compilation unit
//	    CompilationUnit astRoot = parse(unit);
//
//	    //create a ASTRewrite
//	    AST ast = astRoot.getAST();
//	    ASTRewrite rewriter = ASTRewrite.create(ast);
//
//	    //for getting insertion position
//	    TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
//	    MethodDeclaration methodDecl = typeDecl.getMethods()[0];
//	    Block block = methodDecl.getBody();
//
//	    ListRewrite listRewrite = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);
//	    Statement placeHolder = (Statement) rewriter.createStringPlaceholder("//mycomment", ASTNode.EMPTY_STATEMENT);
//	    listRewrite.insertFirst(placeHolder, null);
//
//	    TextEdit edits = rewriter.rewriteAST();
//
//	    // apply the text edits to the compilation unit
//	    Document document = new Document(unit.getSource());
//
//	    edits.apply(document);
//
//	    // this is the code for adding statements
//	    unit.getBuffer().setContents(document.get());
//
//	    System.out.println("done");
//	}
	
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

        String path="E:\\eclipse_work\\test\\src\\test\\hello\\test.java";
        PrintStream stream=null;
        stream=new PrintStream(path);
        stream.print(sourceUnit.toString());
	}
	private  void makeActions() {
		identifyBadSmellsAction = new Action() { //识别循环依赖
			public void run() {
				activeProject = selectedProject;
				
				InputArgs argsObj=new InputArgs("E:\\eclipse_work\\test", "E:\\test\\out");
				SM_Project project = new SM_Project(argsObj);
				//Logger.logFile = getlogFileName(argsObj);
				//TODO: log the version number
				project.parse();
				project.resolve();
				project.computeMetrics();
				project.detectCodeSmells();
//				if (Constants.DEBUG)
//					writeDebugLog(argsObj, project);
				Logger.log("Done.");
				
				
//				String projectPath= activeProject.getProject().getLocation().toString();//获取项目路径
//				IWorkbench wb = PlatformUI.getWorkbench();
//				IProgressService ps = wb.getProgressService();
//				try {
//					ps.busyCursorWhile(new IRunnableWithProgress() {
//						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//							monitor.beginTask("Identification of dependency refactoring opportunities",0);
//							if(monitor.isCanceled())
//								throw new OperationCanceledException();
//							cycle = new Cycle(projectPath);
//							Collection <Dependency> dep = cycle.getCycle();
//							dependencies = dep.toArray(new Dependency[dep.size()]);//得到循环依赖	
//							monitor.worked(1);
//							HashSet<String> callers = cycle.getCallers("org.jfree.experimental.chart.swt.editor.SWTAxisEditor.getInstance");
//							for(String c:callers) {
//								System.out.println("callers: "+c);
//							}
//							
//						}
//					});
//					
//				} catch (InvocationTargetException | InterruptedException e) {
//					// TODO Auto-generated catch block
//					MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
//							"Errors were detected in the project. Fix the errors before refactoring.");
//					e.printStackTrace();
//				}
//				treeViewer.setContentProvider(new ViewContentProvider());
			}
		};
		identifyBadSmellsAction.setToolTipText("Identify Bad Smells");
		identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
//		identifyBadSmellsAction.setEnabled(false);
		
		applyRefactoringAction = new Action() {//重构
			public void run() {
				
				
				System.out.print("start refactoring"+"\n");

				InputArgs argsObj=new InputArgs("E:\\eclipse_work\\test", "E:\\test\\out");
				SM_Project project = new SM_Project(argsObj);
				try {
					MoveFunction(project);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	

				project.resolve();
				//System.out.print(project.getHierarchyGraph().);
				project.computeMetrics();
				//System.out.print("hierar:   "+project.getHierarchyGraph().getConnectedComponnents().get(0).get(0)+"\n");
				project.detectCodeSmells();
				Logger.log("Done.");
				
			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
		getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		
		
		
		
		
		
		
		
		
//		applyRefactoringAction = new Action() {//重构
//			public void run() {
//				
//				
//				System.out.print("start refactoring"+"\n");
//
//				InputArgs argsObj=new InputArgs("E:\\eclipse_work\\test", "E:\\test\\out");
//				SM_Project project = new SM_Project(argsObj);
//				try {
//					test(project);
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//	
//
//				project.resolve();
//				//System.out.print(project.getHierarchyGraph().);
//				project.computeMetrics();
//				//System.out.print("hierar:   "+project.getHierarchyGraph().getConnectedComponnents().get(0).get(0)+"\n");
//				project.detectCodeSmells();
//				Logger.log("Done.");
//				
//			}
//		};
//		applyRefactoringAction.setToolTipText("Apply Refactoring");
//		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//		getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
//		
		
		
		
		
		
//		applyRefactoringAction.setEnabled(false);
		
		doubleClickAction = new Action() {
			public void run() {				

//				
//				System.out.print("start refactoring"+"\n");
//
//				InputArgs argsObj=new InputArgs("E:\\eclipse_work\\test", "E:\\test\\out");
//				SM_Project project = new SM_Project(argsObj);
//				try {
//					MoveFunction(project);
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//	
//
//				project.resolve();
//				//System.out.print(project.getHierarchyGraph().);
//				project.computeMetrics();
//				//System.out.print("hierar:   "+project.getHierarchyGraph().getConnectedComponnents().get(0).get(0)+"\n");
//				project.detectCodeSmells();
//				Logger.log("Done.");
			}
		};

	}
	
	
	
    
	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private ISelectionListener selectionListener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object element = structuredSelection.getFirstElement();
				IJavaProject javaProject = null;
				if(element instanceof IJavaProject) {
					javaProject = (IJavaProject)element;
				}
				else if(element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot)element;
					javaProject = packageFragmentRoot.getJavaProject();
				}
				else if(element instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment)element;
					javaProject = packageFragment.getJavaProject();
				}
				else if(element instanceof ICompilationUnit) {
					ICompilationUnit compilationUnit = (ICompilationUnit)element;
					javaProject = compilationUnit.getJavaProject();
				}
				else if(element instanceof IType) {
					IType type = (IType)element;
					javaProject = type.getJavaProject();
				}
				if(javaProject != null && !javaProject.equals(selectedProject)) {
					selectedProject = javaProject;
					/*if(candidateRefactoringTable != null)
						tableViewer.remove(candidateRefactoringTable);*/
					identifyBadSmellsAction.setEnabled(true);
				}
			}
		}
	};
	
	public String getRelativePath(String absolutePath) {
		String projectPath = activeProject.getProject().getLocation().toString();
		String filePath = absolutePath.replaceAll("\\\\", "/");
		String relativePath = "/"+activeProject.getElementName()+filePath.replaceAll(projectPath, "");
		return relativePath;
	}
	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
	
	public Dependency getParentDependency(DependencyDetail detail) {
		for(Dependency dependency:dependencies) {
			if(dependency.getId()==detail.getId()) {
				return dependency;
			}
		}
		return null;
	}
	public HashSet<String> openChangeFiles(TypeDeclaration typeDeclB) {
		String interfaceName = typeDeclB.resolveBinding().getBinaryName();
		HashSet<String>  strs = cycle.getImplementsRelation(interfaceName);
		for(String str:strs) {
			try {
				IType type = activeProject.findType(str);
				IJavaElement javaElement = type.getCompilationUnit();
				JavaUI.openInEditor(javaElement);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return strs;
	}
}






