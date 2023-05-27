package cn.njust.cy.actions;

import java.text.MessageFormat;
import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PartInitException;

import cn.njust.cy.entity.DependencyDetail;
import depends.matrix.core.DependencyValue;

public class InversionRefactoring extends Refactoring{
	private IFile fileA;
	private IFile fileB;
	private String newFileName;
	
	private DependencyDetail details;
	private Collection<String> changesFile;
	private HashSet<IJavaElement> javaElementsToOpenInEditor;

	public InversionRefactoring(IFile fileA,IFile fileB,String newFileName,DependencyDetail details,Collection<String> changesFile) {
		this.fileA = fileA;
		this.fileB = fileB;
		this.newFileName = "Abstract" + fileA.getName().replace(".java","");//设置新文件的默认名
		this.details = details;
		this.changesFile = changesFile;
		this.javaElementsToOpenInEditor = new HashSet<IJavaElement>();
	}
	
	public void applyRefactoring() { 
		createNewFile(); //1.创建abstractA
		modifySourceClassA();//2.修改class A
		modifySourceClassB();//3.修改class B
		modifyChanges();//4.如果重构后影响其他文件，进行修改
	}
	

	public IPackageFragment getPackage(IFile file) { //得到当前文件所在的package
		IFolder folder = (IFolder) file.getParent();
		IPackageFragment mypackage = (IPackageFragment)JavaCore.create(folder);
		return mypackage;
	}
	
	public CompilationUnit getCompilationUnit(IFile file) {  //得到当前文件的CompilationUnit
		ICompilationUnit IUnit = (ICompilationUnit)JavaCore.create(file);
		CompilationUnit unit = parse(IUnit);
		return unit;
	}
	
	public void createNewFile() {//根据classA创建其抽象
		//获取源文件（fileA）的各种信息
		IPackageFragment sourcePackage = getPackage(fileA);
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnit = parse(sourceIUnit);	
		TypeDeclaration sourceTypeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		MethodDeclaration[] sourceMethodDecls = sourceTypeDecl.getMethods();
		try {
			//创建新的文件
			ICompilationUnit extractIUnit = sourcePackage.createCompilationUnit(newFileName+".java","",false, null);
			//设置其package 
			if(sourceUnit.getPackage() != null) {
				extractIUnit.createPackageDeclaration(sourceUnit.getPackage().getName().toString(), null);
			}
//			String typeStr = "public interface " + interfaceName + " {"+ "\n" + "}";
			String typeStr = "public abstract class " + newFileName + " {"+ "\n" + "}";
			
			Set<String> requiredImportClass = new HashSet<String>(); //需要import的class
			
			if(sourceTypeDecl.getSuperclassType()!=null) { //如果原来的类继承于别的类，让这个类继承
				Type superClass = sourceTypeDecl.getSuperclassType();
				typeStr = "public abstract class " + newFileName +" extends " + superClass + "{"+ "\n" + "}";
				if(!superClass.resolveBinding().getPackage().toString().equals(sourcePackage.toString())) { //如果不是来自一个package，还需要import
					requiredImportClass.add(superClass.resolveBinding().getPackage().getName()+"."+superClass);
				}
			}
			extractIUnit.createType(typeStr,null,true, null);
			IType type = extractIUnit.getType(newFileName);
			
			//创建 abstract method  不是所有的method都提取，只提取需要调用的
			String qualifiedNameA = sourceTypeDecl.resolveBinding().getQualifiedName();
			Set<MethodDeclaration> methodDeclsRequiredAbstract = new HashSet<MethodDeclaration>(); //需要用到的method
			for(DependencyValue value:details.getValues()) {
				if(value.getType().equals("Call")) {
					if(!value.getDetailTo().equals(qualifiedNameA)) {
						String methodName = value.getDetailTo().replaceAll(qualifiedNameA+".","");
						System.out.println("methodName  "+methodName);
						MethodDeclaration methodDecl = findMethodByName(sourceMethodDecls,methodName);
						if(methodDecl!=null) methodDeclsRequiredAbstract.add(methodDecl);
					}
				}
			}
			for(MethodDeclaration methodDecl:methodDeclsRequiredAbstract) {
				String myMethod = "";
				List<Modifier> modifiers = methodDecl.modifiers();
				String parameterStr = "";
				if(methodDecl.parameters()!=null) {
					List<SingleVariableDeclaration> sourceMethodParameterList = methodDecl.parameters();
					for(SingleVariableDeclaration parameter : sourceMethodParameterList) {
						String fileAName = fileA.getName().replace(".java", "");
						SingleVariableDeclaration lastParameter = sourceMethodParameterList.get(sourceMethodParameterList.size()-1);
						if(parameter.getType().toString().equals(fileAName)) {//如果参数类型为本身，则要将参数也换成abstract
							if(parameter.equals(lastParameter)) {//如果为最后一个
								parameterStr = parameterStr + newFileName +" "+ parameter.getName();
							}else {
								parameterStr = parameterStr + newFileName +" "+ parameter.getName() + ", ";
							}//同时也要修改A中该方法的parameter
							AST ast = sourceUnit.getAST();
							ASTRewrite sourceRewriter = ASTRewrite.create(ast);		
							MethodDeclaration sourceMethodDecl = findMethodByName(sourceMethodDecls,methodDecl.getName().toString());
							sourceMethodDecl.accept(new ASTVisitor() {
								public boolean visit(SimpleType node) {
									sourceRewriter.set(node,SimpleType.NAME_PROPERTY, ast.newName(newFileName), null);
									return true;
								}
							});
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
						else {
							if(parameter.equals(lastParameter)) {
								parameterStr = parameterStr + parameter.getType() +" "+ parameter.getName();
							}else {
								parameterStr = parameterStr + parameter.getType() +" "+ parameter.getName() + ", ";
							}
						}
						
					}
				}
				boolean isPrivate = false;
				for(Modifier modifier:modifiers) {
					String keywordStr = modifier.getKeyword().toString();
					if(keywordStr.equals("private")||keywordStr.equals("protected")||keywordStr.equals("static")) isPrivate = true;
					myMethod = myMethod + modifier.getKeyword();
				}
				String throwns = "";
				List<Object> thrownList = methodDecl.thrownExceptions();
				if(thrownList.size()!=0) {
					throwns = " throws ";
					for(Object thrownStr:thrownList) {
						if(thrownList.get(thrownList.size()-1).equals(thrownStr)) {
							throwns = throwns + thrownStr.toString();
						}else {
							throwns = throwns + thrownStr.toString() + ",";
						}
					}
				}
				// 设置为abstract method
//				myMethod = myMethod + " " + methodDecl.getReturnType2()+ " " +methodDecl.getName()
//				+"("+parameterList+")" + throwns + ";";
				myMethod = myMethod +" abstract"+ " " + methodDecl.getReturnType2()+ " " +methodDecl.getName()
								+"("+parameterStr+")" + throwns + ";";
				if(methodDecl.getReturnType2()!=null&&!isPrivate) {//不是构造函数；不是私有函数
					type.createMethod(myMethod, null, true, null);
					System.out.println(myMethod);
				}
			}
		
			//得到需要Import的class
			for(MethodDeclaration methodDecl:methodDeclsRequiredAbstract) {
				ITypeBinding returnType = methodDecl.resolveBinding().getReturnType();
				if(returnType.getPackage()!=null) {
					requiredImportClass.add(returnType.getPackage().getName()+"."+returnType.getName());
				}
				for(ITypeBinding parameterIBinding:methodDecl.resolveBinding().getParameterTypes()) {
					if(parameterIBinding.getPackage()!=null) {
						requiredImportClass.add(parameterIBinding.getPackage().getName()+"."+parameterIBinding.getName());
					}							
				}
				for(ITypeBinding thrownIBinding:methodDecl.resolveBinding().getExceptionTypes()) {
					if(thrownIBinding.getPackage()!=null) {
						requiredImportClass.add(thrownIBinding.getPackage().getName()+"."+thrownIBinding.getName());
					}							
				}
			}
			
			//创建需要Import的class
			for(String importStr:requiredImportClass) {
				String requiredImportpackage = importStr.substring(0, importStr.lastIndexOf("."));
				if(!requiredImportpackage.equals("java.lang")&&!requiredImportpackage.equals(sourcePackage.getElementName()))
//					requiredImportClass.remove(str);
					extractIUnit.createImport(importStr,null, null);
			}
			
			IJavaElement javaElement = extractIUnit;
			javaElementsToOpenInEditor.add(javaElement);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void modifySourceClassA() { //修改classA ：添加interface 
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnit = parse(sourceIUnit);	
		TypeDeclaration typeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		AST ast = sourceUnit.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);								
		//对源文件设置extends class
		sourceRewriter.set(typeDecl,TypeDeclaration.SUPERCLASS_TYPE_PROPERTY,ast.newName(newFileName),null);
		
//		ListRewrite superInterfaceRewrite = sourceRewriter.getListRewrite(typeDecl, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
//		superInterfaceRewrite.insertLast(ast.newName(interfaceName), null);
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
		IJavaElement javaElement = sourceIUnit;
		javaElementsToOpenInEditor.add(javaElement);
	}
	// 对 classB 重命名
	public void modifySourceClassB() {
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileB);
		CompilationUnit sourceUnit = parse(sourceIUnit);	
		IPackageFragment sourcePackage = (IPackageFragment)JavaCore.create((IFolder) fileB.getParent());
		TypeDeclaration typeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		MethodDeclaration[] methodDecls = typeDecl.getMethods();
		AST ast = sourceUnit.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);		
		String packageName = sourcePackage.getElementName();
		String sourceName = fileA.getName().replaceAll(".java", "");
		String className = packageName + "." + sourceIUnit.getElementName().replace(".java", "");
		//如果A和B不在一个package,那么需要移除import A;添加import abstractA;
		IPackageFragment sourcePackageA = (IPackageFragment)JavaCore.create((IFolder) fileA.getParent());
		if(!packageName.equals(sourcePackageA.getElementName())) {
			ListRewrite packageRewrite = sourceRewriter.getListRewrite(sourceUnit, CompilationUnit.IMPORTS_PROPERTY);
//			ImportDeclaration removeImport =  如果需要移除
			String importStr = sourcePackageA.getElementName()+"."+ newFileName;
			ImportDeclaration importDecl = ast.newImportDeclaration();
			importDecl.setName(ast.newName(importStr));
			packageRewrite.insertLast(importDecl, null);
		}
		//重命名
		typeDecl.accept(new ASTVisitor() {
			public boolean visit(SimpleType node) {
//				System.out.println(node.toString());
				if(node.toString().equals(sourceName)) {
					sourceRewriter.set(node,SimpleType.NAME_PROPERTY, ast.newName(newFileName), null);	
				}
				return true;
			}
		});
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
		IJavaElement javaElement = sourceIUnit;
		javaElementsToOpenInEditor.add(javaElement);
	}
	
	public void modifyChanges() {
		if(changesFile!=null) { //不为空，需要修改
			IJavaProject myProject = ((ICompilationUnit)JavaCore.create(fileA)).getJavaProject();//获取当前项目
			for(String str:changesFile) {
				try {
					IType type = myProject.findType(str);
					ICompilationUnit IUnit = type.getCompilationUnit();
					System.out.println("unit: "+IUnit.getElementName());
					IJavaElement javaElement = IUnit;
					javaElementsToOpenInEditor.add(javaElement);
					modifyImplementsChange(IUnit);
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void modifyImplementsChange(ICompilationUnit IUnit) {
		CompilationUnit unit = parse(IUnit);
		TypeDeclaration typeDecl = (TypeDeclaration) unit.types().get(0);
		AST ast = unit.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);
		String sourceName = fileA.getName().replaceAll(".java", "");
		//待修改（重命名的只是接口中修改的地方，同时将将要调用的函数找到，以便提取abstract method）
		typeDecl.accept(new ASTVisitor() {
			public boolean visit(SimpleType node) {
//				System.out.println(node.toString());
				if(node.toString().equals(sourceName)) {
					sourceRewriter.set(node,SimpleType.NAME_PROPERTY, ast.newName(newFileName), null);	
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
	}
	
	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	public String getNewFileName() {
		return newFileName;
	}
	
	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}
	
	public HashSet<IJavaElement> getJavaElementsToOpenInEditor() {
		return javaElementsToOpenInEditor;
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
			applyRefactoring();
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
//			changes.addAll(compilationUnitChanges.values());
//			changes.addAll(createCompilationUnitChanges.values());
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					CompilationUnit sourceCompilationUnit = getCompilationUnit(fileA);
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					TypeDeclaration sourceTypeDecl = (TypeDeclaration) sourceCompilationUnit.types().get(0);
					String projectName = sourceICompilationUnit.getJavaProject().getElementName();
					String description = MessageFormat.format("Refactor from ''{0}''", new Object[] { sourceTypeDecl.getName().getIdentifier()});
					String comment = null;
					return new RefactoringChangeDescriptor(new InversionRefactoringDescriptor(projectName,description,
							comment,fileA,fileB,newFileName,details,changesFile));
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
		return "Inversion Refactoring";
	}
}
