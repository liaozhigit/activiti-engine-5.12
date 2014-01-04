/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.cfg;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.AbstractFormType;
import org.activiti.engine.impl.FormServiceImpl;
import org.activiti.engine.impl.HistoryServiceImpl;
import org.activiti.engine.impl.IdentityServiceImpl;
import org.activiti.engine.impl.ManagementServiceImpl;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.RuntimeServiceImpl;
import org.activiti.engine.impl.ServiceImpl;
import org.activiti.engine.impl.TaskServiceImpl;
import org.activiti.engine.impl.bpmn.data.ItemInstance;
import org.activiti.engine.impl.bpmn.deployer.BpmnDeployer;
import org.activiti.engine.impl.bpmn.parser.BpmnParseHandlers;
import org.activiti.engine.impl.bpmn.parser.BpmnParser;
import org.activiti.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.activiti.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.activiti.engine.impl.bpmn.parser.factory.DefaultListenerFactory;
import org.activiti.engine.impl.bpmn.parser.factory.ListenerFactory;
import org.activiti.engine.impl.bpmn.parser.handler.BoundaryEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.BusinessRuleParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.CallActivityParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.CancelEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.CompensateEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.EndEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ErrorEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.EventBasedGatewayParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.EventSubProcessParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ExclusiveGatewayParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.InclusiveGatewayParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.IntermediateCatchEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.IntermediateThrowEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ManualTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.MessageEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ParallelGatewayParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ProcessParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ReceiveTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ScriptTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.SendTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.SequenceFlowParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ServiceTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.SignalEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.StartEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.SubProcessParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.TaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.TimerEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.TransactionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.UserTaskParseHandler;
import org.activiti.engine.impl.bpmn.webservice.MessageInstance;
import org.activiti.engine.impl.calendar.BusinessCalendarManager;
import org.activiti.engine.impl.calendar.CycleBusinessCalendar;
import org.activiti.engine.impl.calendar.DueDateBusinessCalendar;
import org.activiti.engine.impl.calendar.DurationBusinessCalendar;
import org.activiti.engine.impl.calendar.MapBusinessCalendarManager;
import org.activiti.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.activiti.engine.impl.db.DbIdGenerator;
import org.activiti.engine.impl.db.DbSqlSessionFactory;
import org.activiti.engine.impl.db.IbatisVariableTypeHandler;
import org.activiti.engine.impl.db.upgrade.InstanceUpgrade;
import org.activiti.engine.impl.delegate.DefaultDelegateInterceptor;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.event.CompensationEventHandler;
import org.activiti.engine.impl.event.EventHandler;
import org.activiti.engine.impl.event.MessageEventHandler;
import org.activiti.engine.impl.event.SignalEventHandler;
import org.activiti.engine.impl.form.BooleanFormType;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.FormEngine;
import org.activiti.engine.impl.form.FormTypes;
import org.activiti.engine.impl.form.JuelFormEngine;
import org.activiti.engine.impl.form.LongFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.impl.form.UserFormType;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.impl.history.HistoryManager;
import org.activiti.engine.impl.history.parse.FlowNodeHistoryParseHandler;
import org.activiti.engine.impl.history.parse.ProcessHistoryParseHandler;
import org.activiti.engine.impl.history.parse.StartEventHistoryParseHandler;
import org.activiti.engine.impl.history.parse.UserTaskHistoryParseHandler;
import org.activiti.engine.impl.identity.UserInfoMap;
import org.activiti.engine.impl.identity.UserInfoMapImpl;
import org.activiti.engine.impl.interceptor.CommandContextFactory;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.interceptor.CommandExecutorImpl;
import org.activiti.engine.impl.interceptor.CommandInterceptor;
import org.activiti.engine.impl.interceptor.DelegateInterceptor;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.activiti.engine.impl.jobexecutor.CallerRunsRejectedJobsHandler;
import org.activiti.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.activiti.engine.impl.jobexecutor.DefaultJobExecutor;
import org.activiti.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.jobexecutor.JobHandler;
import org.activiti.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.activiti.engine.impl.jobexecutor.RejectedJobsHandler;
import org.activiti.engine.impl.jobexecutor.TimerActivateProcessDefinitionHandler;
import org.activiti.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.activiti.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.activiti.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.activiti.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.activiti.engine.impl.persistence.GenericManagerFactory;
import org.activiti.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.activiti.engine.impl.persistence.deploy.Deployer;
import org.activiti.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.engine.impl.persistence.entity.AttachmentEntityManager;
import org.activiti.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.engine.impl.persistence.entity.CommentEntityManager;
import org.activiti.engine.impl.persistence.entity.DeploymentEntityManager;
import org.activiti.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.activiti.engine.impl.persistence.entity.GroupEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntityManager;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
import org.activiti.engine.impl.persistence.entity.MembershipEntityManager;
import org.activiti.engine.impl.persistence.entity.ModelEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.activiti.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.engine.impl.persistence.entity.ResourceEntityManager;
import org.activiti.engine.impl.persistence.entity.TableDataManager;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntityManager;
import org.activiti.engine.impl.scripting.BeansResolverFactory;
import org.activiti.engine.impl.scripting.ResolverFactory;
import org.activiti.engine.impl.scripting.ScriptBindingsFactory;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.impl.scripting.VariableScopeResolverFactory;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.impl.util.ReflectUtil;
import org.activiti.engine.impl.variable.BooleanType;
import org.activiti.engine.impl.variable.ByteArrayType;
import org.activiti.engine.impl.variable.CustomObjectType;
import org.activiti.engine.impl.variable.DateType;
import org.activiti.engine.impl.variable.DefaultVariableTypes;
import org.activiti.engine.impl.variable.DoubleType;
import org.activiti.engine.impl.variable.EntityManagerSession;
import org.activiti.engine.impl.variable.EntityManagerSessionFactory;
import org.activiti.engine.impl.variable.IntegerType;
import org.activiti.engine.impl.variable.JPAEntityVariableType;
import org.activiti.engine.impl.variable.LongType;
import org.activiti.engine.impl.variable.NullType;
import org.activiti.engine.impl.variable.SerializableType;
import org.activiti.engine.impl.variable.ShortType;
import org.activiti.engine.impl.variable.StringType;
import org.activiti.engine.impl.variable.VariableType;
import org.activiti.engine.impl.variable.VariableTypes;
import org.activiti.engine.parse.BpmnParseHandler;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.frameworkset.spi.BaseApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frameworkset.common.poolman.ConfigSQLExecutor;


/**
 * @author Tom Baeyens
 */
public abstract class ProcessEngineConfigurationImpl extends ProcessEngineConfiguration {  

  private static Logger log = LoggerFactory.getLogger(ProcessEngineConfigurationImpl.class);
  
  public static final String DB_SCHEMA_UPDATE_CREATE = "create";
  public static final String DB_SCHEMA_UPDATE_DROP_CREATE = "drop-create";

  public static final String DEFAULT_WS_SYNC_FACTORY = "org.activiti.engine.impl.webservice.CxfWebServiceClientFactory";
  
  public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/activiti/db/mapping/mappings.xml";

  // SERVICES /////////////////////////////////////////////////////////////////

  protected RepositoryService repositoryService = new RepositoryServiceImpl();
  protected RuntimeService runtimeService = new RuntimeServiceImpl();
  protected HistoryService historyService = new HistoryServiceImpl();
  protected IdentityService identityService = new IdentityServiceImpl();
  protected TaskService taskService = new TaskServiceImpl();
  protected FormService formService = new FormServiceImpl();
  protected ManagementService managementService = new ManagementServiceImpl();
  /**
   * 用来获取用户账号和账号实名之间的映射关系
   */
  protected UserInfoMap userInfoMap = new UserInfoMapImpl();
  
  // COMMAND EXECUTORS ////////////////////////////////////////////////////////
  
  // Command executor and interceptor stack
  /** the configurable list which will be {@link #initInterceptorChain(java.util.List) processed} to build the {@link #commandExecutorTxRequired} */
  protected List<CommandInterceptor> customPreCommandInterceptorsTxRequired;
  protected List<CommandInterceptor> customPostCommandInterceptorsTxRequired;
  
  protected List<CommandInterceptor> commandInterceptorsTxRequired;

  /** this will be initialized during the configurationComplete() */
  protected CommandExecutor commandExecutorTxRequired;
  
  /** the configurable list which will be {@link #initInterceptorChain(List) processed} to build the {@link #commandExecutorTxRequiresNew} */
  protected List<CommandInterceptor> customPreCommandInterceptorsTxRequiresNew;
  protected List<CommandInterceptor> customPostCommandInterceptorsTxRequiresNew;

  protected List<CommandInterceptor> commandInterceptorsTxRequiresNew;

  /** this will be initialized during the configurationComplete() */
  protected CommandExecutor commandExecutorTxRequiresNew;
  
  // SESSION FACTORIES ////////////////////////////////////////////////////////

  protected List<SessionFactory> customSessionFactories;
  protected DbSqlSessionFactory dbSqlSessionFactory;
  protected Map<Class<?>, SessionFactory> sessionFactories;
  
  // DEPLOYERS ////////////////////////////////////////////////////////////////

  protected List<Deployer> customPreDeployers;
  protected List<Deployer> customPostDeployers;
  protected List<Deployer> deployers;
  protected DeploymentManager deploymentManager;
  
  protected int processDefinitionCacheLimit = -1; // By default, no limit
  protected DeploymentCache<ProcessDefinitionEntity> processDefinitionCache;
  
  protected int knowledgeBaseCacheLimit = -1;
  protected DeploymentCache<Object> knowledgeBaseCache;

  // JOB EXECUTOR /////////////////////////////////////////////////////////////
  
  protected List<JobHandler> customJobHandlers;
  protected Map<String, JobHandler> jobHandlers;
  protected JobExecutor jobExecutor;

  // MYBATIS SQL SESSION FACTORY //////////////////////////////////////////////
  
  protected SqlSessionFactory sqlSessionFactory;
  protected TransactionFactory transactionFactory;


  // ID GENERATOR /////////////////////////////////////////////////////////////
  protected IdGenerator idGenerator;
  protected DataSource idGeneratorDataSource;
  protected String idGeneratorDataSourceJndiName;
  
  // Bpmn parser
  protected List<BpmnParseHandler> preBpmnParseHandlers;
  protected List<BpmnParseHandler> postBpmnParseHandlers;
  protected List<BpmnParseHandler> customDefaultBpmnParseHandlers;
  protected ActivityBehaviorFactory activityBehaviorFactory;
  protected ListenerFactory listenerFactory;
  protected BpmnParseFactory bpmnParseFactory;

  // OTHER ////////////////////////////////////////////////////////////////////
  protected List<FormEngine> customFormEngines;
  protected Map<String, FormEngine> formEngines;

  protected List<AbstractFormType> customFormTypes;
  protected FormTypes formTypes;

  protected List<VariableType> customPreVariableTypes;
  protected List<VariableType> customPostVariableTypes;
  protected VariableTypes variableTypes;
  
  protected ExpressionManager expressionManager;
  protected List<String> customScriptingEngineClasses;
  protected ScriptingEngines scriptingEngines;
  protected List<ResolverFactory> resolverFactories;
  
  protected BusinessCalendarManager businessCalendarManager;

  protected String wsSyncFactoryClassName = DEFAULT_WS_SYNC_FACTORY;

  protected CommandContextFactory commandContextFactory;
  protected TransactionContextFactory transactionContextFactory;
  
  protected HistoryLevel historyLevel;
  
  protected Map<Object, Object> beans;

  protected boolean isDbIdentityUsed = true;
  protected boolean isDbHistoryUsed = true;
  
  protected DelegateInterceptor delegateInterceptor;

  protected CommandInterceptor actualCommandExecutor;
  
  protected RejectedJobsHandler customRejectedJobsHandler;
  
  protected Map<String, EventHandler> eventHandlers;
  protected List<EventHandler> customEventHandlers;

  protected FailedJobCommandFactory failedJobCommandFactory;
  
  protected String databaseTablePrefix = "";
  
  /**
   * Set this to true if you want to have extra checks on the BPMN xml that is parsed.
   * See http://www.jorambarrez.be/blog/2013/02/19/uploading-a-funny-xml-can-bring-down-your-server/
   * 
   * Unfortuantely, this feature is not available on some platforms (JDK 6, JBoss),
   * hence the reason why it is disabled by default. If your platform allows 
   * the use of StaxSource during XML parsing, do enable it.
   */
  protected boolean enableSafeBpmnXml = false;
  
  /**
   * The following settings will determine the amount of entities loaded at once when the engine 
   * needs to load multiple entities (eg. when suspending a process definition with all its process instances).
   * 
   * The default setting is quite low, as not to surprise anyone with sudden memory spikes.
   * Change it to something higher if the environment Activiti runs in allows it.
   */
  protected int batchSizeProcessInstances = 25;
  protected int batchSizeTasks = 25;
  
  /**
   * In some situations you want to set the schema to use for table checks / generation if the database metadata
   * doesn't return that correctly, see https://jira.codehaus.org/browse/ACT-1220,
   * https://jira.codehaus.org/browse/ACT-1062
   */
  protected String databaseSchema = null;
  
  protected boolean isCreateDiagramOnDeploy = true;
  protected InstanceUpgrade instanceUpgrade;
  
  // buildProcessEngine ///////////////////////////////////////////////////////
  
  public ProcessEngine buildProcessEngine() {
    init();
    final ProcessEngine processEngine = new ProcessEngineImpl(this);
    BaseApplicationContext.addShutdownHook(new Runnable(){

		@Override
		public void run() {
			processEngine.close();
		}
		
	});
    return processEngine;
  }
  
  // init /////////////////////////////////////////////////////////////////////
  
  protected void init() {
    initHistoryLevel();
    initExpressionManager();
    initVariableTypes();
    initBeans();
    initFormEngines();
    initFormTypes();
    initScriptingEngines();
    initBusinessCalendarManager();
    initCommandContextFactory();
    initTransactionContextFactory();
    initCommandExecutors();
    initServices();
    initIdGenerator();
    initDeployers();
    initJobExecutor();
    initDataSource();
    initTransactionFactory();
    initSqlSessionFactory();
    initSessionFactories();
    initJpa();
    initDelegateInterceptor();
    initEventHandlers();
    initFailedJobCommandFactory();
    initInstanceUpgrade();
  }
  protected void initInstanceUpgrade()
  {
	  if(instanceUpgrade == null)
	  {
		  instanceUpgrade = new InstanceUpgrade();
		  instanceUpgrade.setExecutor(new ConfigSQLExecutor("org/activiti/engine/impl/db/upgrade/upgradesql.xml"));
		  
	  }
	  instanceUpgrade.setTaskService(this.taskService);
	  instanceUpgrade.setRuntimeService(this.runtimeService);
  }

  // failedJobCommandFactory ////////////////////////////////////////////////////////
  
  protected void initFailedJobCommandFactory() {
    if (failedJobCommandFactory == null) {
      failedJobCommandFactory = new DefaultFailedJobCommandFactory();
    }
  }

  // command executors ////////////////////////////////////////////////////////
  
  protected abstract Collection< ? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequired();
  protected abstract Collection< ? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequiresNew();
  
  protected void initCommandExecutors() {
    initActualCommandExecutor();
    initCommandInterceptorsTxRequired();
    initCommandExecutorTxRequired();
    initCommandInterceptorsTxRequiresNew();
    initCommandExecutorTxRequiresNew();
  }

  protected void initActualCommandExecutor() {
    actualCommandExecutor = new CommandExecutorImpl();
  }

  protected void initCommandInterceptorsTxRequired() {
    if (commandInterceptorsTxRequired==null) {
      if (customPreCommandInterceptorsTxRequired!=null) {
        commandInterceptorsTxRequired = new ArrayList<CommandInterceptor>(customPreCommandInterceptorsTxRequired);
      } else {
        commandInterceptorsTxRequired = new ArrayList<CommandInterceptor>();
      }
      commandInterceptorsTxRequired.addAll(getDefaultCommandInterceptorsTxRequired());
      if (customPostCommandInterceptorsTxRequired!=null) {
        commandInterceptorsTxRequired.addAll(customPostCommandInterceptorsTxRequired);
      }
      commandInterceptorsTxRequired.add(actualCommandExecutor);
    }
  }

  protected void initCommandInterceptorsTxRequiresNew() {
    if (commandInterceptorsTxRequiresNew==null) {
      if (customPreCommandInterceptorsTxRequiresNew!=null) {
        commandInterceptorsTxRequiresNew = new ArrayList<CommandInterceptor>(customPreCommandInterceptorsTxRequiresNew);
      } else {
        commandInterceptorsTxRequiresNew = new ArrayList<CommandInterceptor>();
      }
      commandInterceptorsTxRequiresNew.addAll(getDefaultCommandInterceptorsTxRequiresNew());
      if (customPostCommandInterceptorsTxRequiresNew!=null) {
        commandInterceptorsTxRequiresNew.addAll(customPostCommandInterceptorsTxRequiresNew);
      }
      commandInterceptorsTxRequiresNew.add(actualCommandExecutor);
    }
  }

  protected void initCommandExecutorTxRequired() {
    if (commandExecutorTxRequired==null) {
      commandExecutorTxRequired = initInterceptorChain(commandInterceptorsTxRequired);
    }
  }

  protected void initCommandExecutorTxRequiresNew() {
    if (commandExecutorTxRequiresNew==null) {
      commandExecutorTxRequiresNew = initInterceptorChain(commandInterceptorsTxRequiresNew);
    }
  }

  protected CommandInterceptor initInterceptorChain(List<CommandInterceptor> chain) {
    if (chain==null || chain.isEmpty()) {
      throw new ActivitiException("invalid command interceptor chain configuration: "+chain);
    }
    for (int i = 0; i < chain.size()-1; i++) {
      chain.get(i).setNext( chain.get(i+1) );
    }
    return chain.get(0);
  }
  
  // services /////////////////////////////////////////////////////////////////
  
  protected void initServices() {
    initService(repositoryService);
    repositoryService.setProcessEngineConfigurationImpl(this);
    initService(runtimeService);
    initService(historyService);
    initService(identityService);
    initService(taskService);
    initService(formService);
    initService(managementService);
  }

  protected void initService(Object service) {
    if (service instanceof ServiceImpl) {
      ((ServiceImpl)service).setCommandExecutor(commandExecutorTxRequired);
    }
  }
  
  // DataSource ///////////////////////////////////////////////////////////////
  
  protected void initDataSource() {
    if (dataSource==null) {
      if (dataSourceJndiName!=null) {
        try {
          dataSource = (DataSource) new InitialContext().lookup(dataSourceJndiName);
        } catch (Exception e) {
          throw new ActivitiException("couldn't lookup datasource from "+dataSourceJndiName+": "+e.getMessage(), e);
        }
        
      } else if (jdbcUrl!=null) {
        if ( (jdbcDriver==null) || (jdbcUrl==null) || (jdbcUsername==null) ) {
          throw new ActivitiException("DataSource or JDBC properties have to be specified in a process engine configuration");
        }
        
        log.debug("initializing datasource to db: {}", jdbcUrl);
        
        PooledDataSource pooledDataSource = 
          new PooledDataSource(ReflectUtil.getClassLoader(), jdbcDriver, jdbcUrl, jdbcUsername, jdbcPassword );
        
        if (jdbcMaxActiveConnections > 0) {
          pooledDataSource.setPoolMaximumActiveConnections(jdbcMaxActiveConnections);
        }
        if (jdbcMaxIdleConnections > 0) {
          pooledDataSource.setPoolMaximumIdleConnections(jdbcMaxIdleConnections);
        }
        if (jdbcMaxCheckoutTime > 0) {
          pooledDataSource.setPoolMaximumCheckoutTime(jdbcMaxCheckoutTime);
        }
        if (jdbcMaxWaitTime > 0) {
          pooledDataSource.setPoolTimeToWait(jdbcMaxWaitTime);
        }
        if (jdbcPingEnabled == true) {
          pooledDataSource.setPoolPingEnabled(true);
          if (jdbcPingQuery != null) {
            pooledDataSource.setPoolPingQuery(jdbcPingQuery);
          }
          pooledDataSource.setPoolPingConnectionsNotUsedFor(jdbcPingConnectionNotUsedFor);
        }        
        dataSource = pooledDataSource;
      }
      
      if (dataSource instanceof PooledDataSource) {
        // ACT-233: connection pool of Ibatis is not properely initialized if this is not called!
        ((PooledDataSource)dataSource).forceCloseAll();
      }
    }

    if (databaseType == null) {
      initDatabaseType();
    }
  }
  
  protected static Properties databaseTypeMappings = getDefaultDatabaseTypeMappings();
  
  protected static Properties getDefaultDatabaseTypeMappings() {
    Properties databaseTypeMappings = new Properties();
    databaseTypeMappings.setProperty("H2","h2");
    databaseTypeMappings.setProperty("MySQL","mysql");
    databaseTypeMappings.setProperty("Oracle","oracle");
    databaseTypeMappings.setProperty("PostgreSQL","postgres");
    databaseTypeMappings.setProperty("Microsoft SQL Server","mssql");
    databaseTypeMappings.setProperty("DB2","db2");
    databaseTypeMappings.setProperty("DB2","db2");
    databaseTypeMappings.setProperty("DB2/NT","db2");
    databaseTypeMappings.setProperty("DB2/NT64","db2");
    databaseTypeMappings.setProperty("DB2 UDP","db2");
    databaseTypeMappings.setProperty("DB2/LINUX","db2");
    databaseTypeMappings.setProperty("DB2/LINUX390","db2");
    databaseTypeMappings.setProperty("DB2/LINUXX8664","db2");
    databaseTypeMappings.setProperty("DB2/LINUXZ64","db2");
    databaseTypeMappings.setProperty("DB2/400 SQL","db2");
    databaseTypeMappings.setProperty("DB2/6000","db2");
    databaseTypeMappings.setProperty("DB2 UDB iSeries","db2");
    databaseTypeMappings.setProperty("DB2/AIX64","db2");
    databaseTypeMappings.setProperty("DB2/HPUX","db2");
    databaseTypeMappings.setProperty("DB2/HP64","db2");
    databaseTypeMappings.setProperty("DB2/SUN","db2");
    databaseTypeMappings.setProperty("DB2/SUN64","db2");
    databaseTypeMappings.setProperty("DB2/PTX","db2");
    databaseTypeMappings.setProperty("DB2/2","db2");
    return databaseTypeMappings;
  }

  public void initDatabaseType() {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      DatabaseMetaData databaseMetaData = connection.getMetaData();
      String databaseProductName = databaseMetaData.getDatabaseProductName();
      log.debug("database product name: '{}'", databaseProductName);
      databaseType = databaseTypeMappings.getProperty(databaseProductName);
      if (databaseType==null) {
        throw new ActivitiException("couldn't deduct database type from database product name '"+databaseProductName+"'");
      }
      log.debug("using database type: {}", databaseType);

    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (connection!=null) {
          connection.close();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
  
  // myBatis SqlSessionFactory ////////////////////////////////////////////////
  
  protected void initTransactionFactory() {
    if (transactionFactory==null) {
      if (transactionsExternallyManaged) {
        transactionFactory = new ManagedTransactionFactory();
      } else {
        transactionFactory = new JdbcTransactionFactory();
      }
    }
  }

  protected void initSqlSessionFactory() {
    if (sqlSessionFactory==null) {
      InputStream inputStream = null;
      try {
        inputStream = getMyBatisXmlConfigurationSteam();

        // update the jdbc parameters to the configured ones...
        Environment environment = new Environment("default", transactionFactory, dataSource);
        Reader reader = new InputStreamReader(inputStream);
        Properties properties = new Properties();
        properties.put("prefix", databaseTablePrefix);
        if(databaseType != null) {
          properties.put("limitBefore" , DbSqlSessionFactory.databaseSpecificLimitBeforeStatements.get(databaseType));
          properties.put("limitAfter" , DbSqlSessionFactory.databaseSpecificLimitAfterStatements.get(databaseType));
          properties.put("limitBetween" , DbSqlSessionFactory.databaseSpecificLimitBetweenStatements.get(databaseType));
          properties.put("orderBy" , DbSqlSessionFactory.databaseSpecificOrderByStatements.get(databaseType));
        }
        XMLConfigBuilder parser = new XMLConfigBuilder(reader,"", properties);
        Configuration configuration = parser.getConfiguration();
        configuration.setEnvironment(environment);
        configuration.getTypeHandlerRegistry().register(VariableType.class, JdbcType.VARCHAR, new IbatisVariableTypeHandler());
        configuration = parser.parse();

        sqlSessionFactory = new DefaultSqlSessionFactory(configuration);

      } catch (Exception e) {
        throw new ActivitiException("Error while building ibatis SqlSessionFactory: " + e.getMessage(), e);
      } finally {
        IoUtil.closeSilently(inputStream);
      }
    }
  }
  
  protected InputStream getMyBatisXmlConfigurationSteam() {
    return ReflectUtil.getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
  }

  // session factories ////////////////////////////////////////////////////////
  
  protected void initSessionFactories() {
    if (sessionFactories==null) {
      sessionFactories = new HashMap<Class<?>, SessionFactory>();

      dbSqlSessionFactory = new DbSqlSessionFactory();
      dbSqlSessionFactory.setDatabaseType(databaseType);
      dbSqlSessionFactory.setIdGenerator(idGenerator);
      dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
      dbSqlSessionFactory.setDbIdentityUsed(isDbIdentityUsed);
      dbSqlSessionFactory.setDbHistoryUsed(isDbHistoryUsed);
      dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
      dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
      dbSqlSessionFactory.setDataSource(dataSource);
      addSessionFactory(dbSqlSessionFactory);
      
      addSessionFactory(new GenericManagerFactory(AttachmentEntityManager.class));
      addSessionFactory(new GenericManagerFactory(CommentEntityManager.class));
      addSessionFactory(new GenericManagerFactory(DeploymentEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ModelEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ExecutionEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricActivityInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricDetailEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricProcessInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricVariableInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricTaskInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(IdentityInfoEntityManager.class));
      addSessionFactory(new GenericManagerFactory(IdentityLinkEntityManager.class));
      addSessionFactory(new GenericManagerFactory(JobEntityManager.class));
      addSessionFactory(new GenericManagerFactory(GroupEntityManager.class));
      addSessionFactory(new GenericManagerFactory(MembershipEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ProcessDefinitionEntityManager.class));
      addSessionFactory(new GenericManagerFactory(PropertyEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ResourceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ByteArrayEntityManager.class));
      addSessionFactory(new GenericManagerFactory(TableDataManager.class));
      addSessionFactory(new GenericManagerFactory(TaskEntityManager.class));
      addSessionFactory(new GenericManagerFactory(UserEntityManager.class));
      addSessionFactory(new GenericManagerFactory(VariableInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(EventSubscriptionEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoryManager.class));
    }
    if (customSessionFactories!=null) {
      for (SessionFactory sessionFactory: customSessionFactories) {
        addSessionFactory(sessionFactory);
      }
    }
  }
  
  protected void addSessionFactory(SessionFactory sessionFactory) {
    sessionFactories.put(sessionFactory.getSessionType(), sessionFactory);
  }
  
  // deployers ////////////////////////////////////////////////////////////////
  
  protected void initDeployers() {
    if (this.deployers==null) {
      this.deployers = new ArrayList<Deployer>();
      if (customPreDeployers!=null) {
        this.deployers.addAll(customPreDeployers);
      }
      this.deployers.addAll(getDefaultDeployers());
      if (customPostDeployers!=null) {
        this.deployers.addAll(customPostDeployers);
      }
    }
    if (deploymentManager==null) {
      deploymentManager = new DeploymentManager();
      deploymentManager.setDeployers(deployers);
      
      // Process Definition cache
      if (processDefinitionCache == null) {
        if (processDefinitionCacheLimit <= 0) {
          processDefinitionCache = new DefaultDeploymentCache<ProcessDefinitionEntity>();
        } else {
          processDefinitionCache = new DefaultDeploymentCache<ProcessDefinitionEntity>(processDefinitionCacheLimit);
        }
      } 
      
      // Knowledge base cache (used for Drools business task)
      if (knowledgeBaseCache == null) {
        if (knowledgeBaseCacheLimit <= 0) {
          knowledgeBaseCache = new DefaultDeploymentCache<Object>();
        } else {
          knowledgeBaseCache = new DefaultDeploymentCache<Object>(knowledgeBaseCacheLimit);
        }
      }
      
      deploymentManager.setProcessDefinitionCache(processDefinitionCache);
      deploymentManager.setKnowledgeBaseCache(knowledgeBaseCache);
    }
  }

  protected Collection< ? extends Deployer> getDefaultDeployers() {
    List<Deployer> defaultDeployers = new ArrayList<Deployer>();

    BpmnDeployer bpmnDeployer = new BpmnDeployer();
    bpmnDeployer.setExpressionManager(expressionManager);
    bpmnDeployer.setIdGenerator(idGenerator);
    
    if (bpmnParseFactory == null) {
      bpmnParseFactory = new DefaultBpmnParseFactory();
    }
    
    if (activityBehaviorFactory == null) {
      DefaultActivityBehaviorFactory defaultActivityBehaviorFactory = new DefaultActivityBehaviorFactory();
      defaultActivityBehaviorFactory.setExpressionManager(expressionManager);
      activityBehaviorFactory = defaultActivityBehaviorFactory;
    }
    
    if (listenerFactory == null) {
      DefaultListenerFactory defaultListenerFactory = new DefaultListenerFactory();
      defaultListenerFactory.setExpressionManager(expressionManager);
      listenerFactory = defaultListenerFactory;
    }
    
    BpmnParser bpmnParser = new BpmnParser();
    bpmnParser.setExpressionManager(expressionManager);
    bpmnParser.setBpmnParseFactory(bpmnParseFactory);
    bpmnParser.setActivityBehaviorFactory(activityBehaviorFactory);
    bpmnParser.setListenerFactory(listenerFactory);
    
    List<BpmnParseHandler> parseHandlers = new ArrayList<BpmnParseHandler>();
    if(getPreBpmnParseHandlers() != null) {
      parseHandlers.addAll(getPreBpmnParseHandlers());
    }
    parseHandlers.addAll(getDefaultBpmnParseHandlers());
    if(getPostBpmnParseHandlers() != null) {
      parseHandlers.addAll(getPostBpmnParseHandlers());
    }
    
    BpmnParseHandlers bpmnParseHandlers = new BpmnParseHandlers();
    bpmnParseHandlers.addHandlers(parseHandlers);
    bpmnParser.setBpmnParserHandlers(bpmnParseHandlers);
    
    bpmnDeployer.setBpmnParser(bpmnParser);
    
    defaultDeployers.add(bpmnDeployer);
    return defaultDeployers;
  }
  
  protected List<BpmnParseHandler> getDefaultBpmnParseHandlers() {
    
    // Alpabetic list of default parse handler classes
    List<BpmnParseHandler> bpmnParserHandlers = new ArrayList<BpmnParseHandler>();
    bpmnParserHandlers.add(new BoundaryEventParseHandler());
    bpmnParserHandlers.add(new BusinessRuleParseHandler());
    bpmnParserHandlers.add(new CallActivityParseHandler());
    bpmnParserHandlers.add(new CancelEventDefinitionParseHandler());
    bpmnParserHandlers.add(new CompensateEventDefinitionParseHandler());
    bpmnParserHandlers.add(new EndEventParseHandler());
    bpmnParserHandlers.add(new ErrorEventDefinitionParseHandler());
    bpmnParserHandlers.add(new EventBasedGatewayParseHandler());
    bpmnParserHandlers.add(new ExclusiveGatewayParseHandler());
    bpmnParserHandlers.add(new InclusiveGatewayParseHandler());
    bpmnParserHandlers.add(new IntermediateCatchEventParseHandler());
    bpmnParserHandlers.add(new IntermediateThrowEventParseHandler());
    bpmnParserHandlers.add(new ManualTaskParseHandler());
    bpmnParserHandlers.add(new MessageEventDefinitionParseHandler());
    bpmnParserHandlers.add(new ParallelGatewayParseHandler());
    bpmnParserHandlers.add(new ProcessParseHandler());
    bpmnParserHandlers.add(new ReceiveTaskParseHandler());
    bpmnParserHandlers.add(new ScriptTaskParseHandler());
    bpmnParserHandlers.add(new SendTaskParseHandler());
    bpmnParserHandlers.add(new SequenceFlowParseHandler());
    bpmnParserHandlers.add(new ServiceTaskParseHandler());
    bpmnParserHandlers.add(new SignalEventDefinitionParseHandler());
    bpmnParserHandlers.add(new StartEventParseHandler());
    bpmnParserHandlers.add(new SubProcessParseHandler());
    bpmnParserHandlers.add(new EventSubProcessParseHandler());
    bpmnParserHandlers.add(new TaskParseHandler());
    bpmnParserHandlers.add(new TimerEventDefinitionParseHandler());
    bpmnParserHandlers.add(new TransactionParseHandler());
    bpmnParserHandlers.add(new UserTaskParseHandler());
    
    // Replace any default handler if the user wants to replace them
    if (customDefaultBpmnParseHandlers != null) {
      
      Map<Class<?>, BpmnParseHandler> customParseHandlerMap = new HashMap<Class<?>, BpmnParseHandler>();
      for (BpmnParseHandler bpmnParseHandler : customDefaultBpmnParseHandlers) {
        for (Class<?> handledType : bpmnParseHandler.getHandledTypes()) {
          customParseHandlerMap.put(handledType, bpmnParseHandler);
        }
      }
      
      for (int i=0; i<bpmnParserHandlers.size(); i++) {
        // All the default handlers support only one type
        BpmnParseHandler defaultBpmnParseHandler = bpmnParserHandlers.get(i);
        if (defaultBpmnParseHandler.getHandledTypes().size() != 1) {
          StringBuilder supportedTypes = new StringBuilder();
          for (Class<?> type : defaultBpmnParseHandler.getHandledTypes()) {
            supportedTypes.append(" " + type.getCanonicalName() + " ");
          }
          throw new ActivitiException("The default BPMN parse handlers should only support one type, but " + defaultBpmnParseHandler.getClass() 
                  + " supports " + supportedTypes.toString() + ". This is likely a programmatic error");
        } else {
          Class<?> handledType = defaultBpmnParseHandler.getHandledTypes().iterator().next();
          if (customParseHandlerMap.containsKey(handledType)) {
            BpmnParseHandler newBpmnParseHandler = customParseHandlerMap.get(handledType);
            log.info("Replacing default BpmnParseHandler " + defaultBpmnParseHandler.getClass().getName() + " with " + newBpmnParseHandler.getClass().getName());
            bpmnParserHandlers.set(i, newBpmnParseHandler);
          }
        }
      }
    }
    
    // History
    for (BpmnParseHandler handler : getDefaultHistoryParseHandlers()) {
      bpmnParserHandlers.add(handler);
    }
    
    return bpmnParserHandlers;
  }
  
  protected List<BpmnParseHandler> getDefaultHistoryParseHandlers() {
    List<BpmnParseHandler> parseHandlers = new ArrayList<BpmnParseHandler>();
    parseHandlers.add(new FlowNodeHistoryParseHandler());
    parseHandlers.add(new ProcessHistoryParseHandler());
    parseHandlers.add(new StartEventHistoryParseHandler());
    parseHandlers.add(new UserTaskHistoryParseHandler());
    return parseHandlers;
  }

  // job executor /////////////////////////////////////////////////////////////
  
  protected void initJobExecutor() {
    if (jobExecutor==null) {
      jobExecutor = new DefaultJobExecutor();
    }

    jobHandlers = new HashMap<String, JobHandler>();
    TimerExecuteNestedActivityJobHandler timerExecuteNestedActivityJobHandler = new TimerExecuteNestedActivityJobHandler();
    jobHandlers.put(timerExecuteNestedActivityJobHandler.getType(), timerExecuteNestedActivityJobHandler);

    TimerCatchIntermediateEventJobHandler timerCatchIntermediateEvent = new TimerCatchIntermediateEventJobHandler();
    jobHandlers.put(timerCatchIntermediateEvent.getType(), timerCatchIntermediateEvent);

    TimerStartEventJobHandler timerStartEvent = new TimerStartEventJobHandler();
    jobHandlers.put(timerStartEvent.getType(), timerStartEvent);
    
    AsyncContinuationJobHandler asyncContinuationJobHandler = new AsyncContinuationJobHandler();
    jobHandlers.put(asyncContinuationJobHandler.getType(), asyncContinuationJobHandler);
    
    ProcessEventJobHandler processEventJobHandler = new ProcessEventJobHandler();
    jobHandlers.put(processEventJobHandler.getType(), processEventJobHandler);
    
    TimerSuspendProcessDefinitionHandler suspendProcessDefinitionHandler = new TimerSuspendProcessDefinitionHandler();
    jobHandlers.put(suspendProcessDefinitionHandler.getType(), suspendProcessDefinitionHandler);
    
    TimerActivateProcessDefinitionHandler activateProcessDefinitionHandler = new TimerActivateProcessDefinitionHandler();
    jobHandlers.put(activateProcessDefinitionHandler.getType(), activateProcessDefinitionHandler);
    
    // if we have custom job handlers, register them
    if (getCustomJobHandlers()!=null) {
      for (JobHandler customJobHandler : getCustomJobHandlers()) {
        jobHandlers.put(customJobHandler.getType(), customJobHandler);      
      }
    }

    jobExecutor.setCommandExecutor(commandExecutorTxRequired);
    jobExecutor.setAutoActivate(jobExecutorActivate);
    
    if(jobExecutor.getRejectedJobsHandler() == null) {
      if(customRejectedJobsHandler != null) {
        jobExecutor.setRejectedJobsHandler(customRejectedJobsHandler);
      } else {
        jobExecutor.setRejectedJobsHandler(new CallerRunsRejectedJobsHandler());
      }
    }
    
  }
  
  // history //////////////////////////////////////////////////////////////////
  
  public void initHistoryLevel() {
    historyLevel = HistoryLevel.getHistoryLevelForKey(getHistory());
  }
  
  // id generator /////////////////////////////////////////////////////////////
  
  protected void initIdGenerator() {
    if (idGenerator==null) {
      CommandExecutor idGeneratorCommandExecutor = null;
      if (idGeneratorDataSource!=null) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(idGeneratorDataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_FALSE);
        processEngineConfiguration.init();
        idGeneratorCommandExecutor = processEngineConfiguration.getCommandExecutorTxRequiresNew();
      } else if (idGeneratorDataSourceJndiName!=null) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSourceJndiName(idGeneratorDataSourceJndiName);
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_FALSE);
        processEngineConfiguration.init();
        idGeneratorCommandExecutor = processEngineConfiguration.getCommandExecutorTxRequiresNew();
      } else {
        idGeneratorCommandExecutor = commandExecutorTxRequiresNew;
      }
      
      DbIdGenerator dbIdGenerator = new DbIdGenerator();
      dbIdGenerator.setIdBlockSize(idBlockSize);
      dbIdGenerator.setCommandExecutor(idGeneratorCommandExecutor);
      idGenerator = dbIdGenerator;
    }
  }

  // OTHER ////////////////////////////////////////////////////////////////////
  
  protected void initCommandContextFactory() {
    if (commandContextFactory==null) {
      commandContextFactory = new CommandContextFactory();
      commandContextFactory.setProcessEngineConfiguration(this);
    }
  }

  protected void initTransactionContextFactory() {
    if (transactionContextFactory==null) {
      transactionContextFactory = new StandaloneMybatisTransactionContextFactory();
    }
  }

  protected void initVariableTypes() {
    if (variableTypes==null) {
      variableTypes = new DefaultVariableTypes();
      if (customPreVariableTypes!=null) {
        for (VariableType customVariableType: customPreVariableTypes) {
          variableTypes.addType(customVariableType);
        }
      }
      variableTypes.addType(new NullType());
      variableTypes.addType(new StringType());
      variableTypes.addType(new BooleanType());
      variableTypes.addType(new ShortType());
      variableTypes.addType(new IntegerType());
      variableTypes.addType(new LongType());
      variableTypes.addType(new DateType());
      variableTypes.addType(new DoubleType());
      variableTypes.addType(new ByteArrayType());
      variableTypes.addType(new SerializableType());
      variableTypes.addType(new CustomObjectType("item", ItemInstance.class));
      variableTypes.addType(new CustomObjectType("message", MessageInstance.class));
      if (customPostVariableTypes!=null) {
        for (VariableType customVariableType: customPostVariableTypes) {
          variableTypes.addType(customVariableType);
        }
      }
    }
  }

  protected void initFormEngines() {
    if (formEngines==null) {
      formEngines = new HashMap<String, FormEngine>();
      FormEngine defaultFormEngine = new JuelFormEngine();
      formEngines.put(null, defaultFormEngine); // default form engine is looked up with null
      formEngines.put(defaultFormEngine.getName(), defaultFormEngine);
    }
    if (customFormEngines!=null) {
      for (FormEngine formEngine: customFormEngines) {
        formEngines.put(formEngine.getName(), formEngine);
      }
    }
  }

  protected void initFormTypes() {
    if (formTypes==null) {
      formTypes = new FormTypes();
      formTypes.addFormType(new StringFormType());
      formTypes.addFormType(new LongFormType());
      formTypes.addFormType(new DateFormType("dd/MM/yyyy"));
      formTypes.addFormType(new BooleanFormType());
      formTypes.addFormType(new UserFormType());
    }
    if (customFormTypes!=null) {
      for (AbstractFormType customFormType: customFormTypes) {
        formTypes.addFormType(customFormType);
      }
    }
  }

  protected void initScriptingEngines() {
    if (resolverFactories==null) {
      resolverFactories = new ArrayList<ResolverFactory>();
      resolverFactories.add(new VariableScopeResolverFactory());
      resolverFactories.add(new BeansResolverFactory());
    }
    if (scriptingEngines==null) {
      scriptingEngines = new ScriptingEngines(new ScriptBindingsFactory(resolverFactories));
    }
  }

  protected void initExpressionManager() {
    if (expressionManager==null) {
//      expressionManager = new ExpressionManager(beans);
    	expressionManager = new BBossExpressionManager(this.beans);
    }
  }

  protected void initBusinessCalendarManager() {
    if (businessCalendarManager==null) {
      MapBusinessCalendarManager mapBusinessCalendarManager = new MapBusinessCalendarManager();
      mapBusinessCalendarManager.addBusinessCalendar(DurationBusinessCalendar.NAME, new DurationBusinessCalendar());
      mapBusinessCalendarManager.addBusinessCalendar(DueDateBusinessCalendar.NAME, new DueDateBusinessCalendar());
      mapBusinessCalendarManager.addBusinessCalendar(CycleBusinessCalendar.NAME, new CycleBusinessCalendar());

      businessCalendarManager = mapBusinessCalendarManager;
    }
  }
  
  protected void initDelegateInterceptor() {
    if(delegateInterceptor == null) {
      delegateInterceptor = new DefaultDelegateInterceptor();
    }
  }
  
  protected void initEventHandlers() {
    if(eventHandlers == null) {
      eventHandlers = new HashMap<String, EventHandler>();
      
      SignalEventHandler signalEventHander = new SignalEventHandler();
      eventHandlers.put(signalEventHander.getEventHandlerType(), signalEventHander);
      
      CompensationEventHandler compensationEventHandler = new CompensationEventHandler();
      eventHandlers.put(compensationEventHandler.getEventHandlerType(), compensationEventHandler);
      
      MessageEventHandler messageEventHandler = new MessageEventHandler();
      eventHandlers.put(messageEventHandler.getEventHandlerType(), messageEventHandler);
      
    }
    if(customEventHandlers != null) {
      for (EventHandler eventHandler : customEventHandlers) {
        eventHandlers.put(eventHandler.getEventHandlerType(), eventHandler);        
      }
    }
  }
  
  // JPA //////////////////////////////////////////////////////////////////////
  
  protected void initJpa() {
    if(jpaPersistenceUnitName!=null) {
      jpaEntityManagerFactory = JpaHelper.createEntityManagerFactory(jpaPersistenceUnitName);
    }
    if(jpaEntityManagerFactory!=null) {
      sessionFactories.put(EntityManagerSession.class, new EntityManagerSessionFactory(jpaEntityManagerFactory, jpaHandleTransaction, jpaCloseEntityManager));
      VariableType jpaType = variableTypes.getVariableType(JPAEntityVariableType.TYPE_NAME);
      // Add JPA-type
      if(jpaType == null) {
        // We try adding the variable right before SerializableType, if available
        int serializableIndex = variableTypes.getTypeIndex(SerializableType.TYPE_NAME);
        if(serializableIndex > -1) {
          variableTypes.addType(new JPAEntityVariableType(), serializableIndex);
        } else {
          variableTypes.addType(new JPAEntityVariableType());
        }        
      }
    }
  }
  
  protected void initBeans() {
    if (beans == null) {
      beans = new HashMap<Object, Object>();
    }
  }

  // getters and setters //////////////////////////////////////////////////////
  
  public String getProcessEngineName() {
    return processEngineName;
  }

  public HistoryLevel getHistoryLevel() {
    return historyLevel;
  }
  
  public void setHistoryLevel(HistoryLevel historyLevel) {
    this.historyLevel = historyLevel;
  }

  public ProcessEngineConfigurationImpl setProcessEngineName(String processEngineName) {
    this.processEngineName = processEngineName;
    return this;
  }
  
  public List<CommandInterceptor> getCustomPreCommandInterceptorsTxRequired() {
    return customPreCommandInterceptorsTxRequired;
  }
  
  public ProcessEngineConfigurationImpl setCustomPreCommandInterceptorsTxRequired(List<CommandInterceptor> customPreCommandInterceptorsTxRequired) {
    this.customPreCommandInterceptorsTxRequired = customPreCommandInterceptorsTxRequired;
    return this;
  }
  
  public List<CommandInterceptor> getCustomPostCommandInterceptorsTxRequired() {
    return customPostCommandInterceptorsTxRequired;
  }
  
  public ProcessEngineConfigurationImpl setCustomPostCommandInterceptorsTxRequired(List<CommandInterceptor> customPostCommandInterceptorsTxRequired) {
    this.customPostCommandInterceptorsTxRequired = customPostCommandInterceptorsTxRequired;
    return this;
  }
  
  public List<CommandInterceptor> getCommandInterceptorsTxRequired() {
    return commandInterceptorsTxRequired;
  }
  
  public ProcessEngineConfigurationImpl setCommandInterceptorsTxRequired(List<CommandInterceptor> commandInterceptorsTxRequired) {
    this.commandInterceptorsTxRequired = commandInterceptorsTxRequired;
    return this;
  }
  
  public CommandExecutor getCommandExecutorTxRequired() {
    return commandExecutorTxRequired;
  }
  
  public ProcessEngineConfigurationImpl setCommandExecutorTxRequired(CommandExecutor commandExecutorTxRequired) {
    this.commandExecutorTxRequired = commandExecutorTxRequired;
    return this;
  }
  
  public List<CommandInterceptor> getCustomPreCommandInterceptorsTxRequiresNew() {
    return customPreCommandInterceptorsTxRequiresNew;
  }
  
  public ProcessEngineConfigurationImpl setCustomPreCommandInterceptorsTxRequiresNew(List<CommandInterceptor> customPreCommandInterceptorsTxRequiresNew) {
    this.customPreCommandInterceptorsTxRequiresNew = customPreCommandInterceptorsTxRequiresNew;
    return this;
  }
  
  public List<CommandInterceptor> getCustomPostCommandInterceptorsTxRequiresNew() {
    return customPostCommandInterceptorsTxRequiresNew;
  }
  
  public ProcessEngineConfigurationImpl setCustomPostCommandInterceptorsTxRequiresNew(List<CommandInterceptor> customPostCommandInterceptorsTxRequiresNew) {
    this.customPostCommandInterceptorsTxRequiresNew = customPostCommandInterceptorsTxRequiresNew;
    return this;
  }
  
  public List<CommandInterceptor> getCommandInterceptorsTxRequiresNew() {
    return commandInterceptorsTxRequiresNew;
  }
  
  public ProcessEngineConfigurationImpl setCommandInterceptorsTxRequiresNew(List<CommandInterceptor> commandInterceptorsTxRequiresNew) {
    this.commandInterceptorsTxRequiresNew = commandInterceptorsTxRequiresNew;
    return this;
  }
  
  public CommandExecutor getCommandExecutorTxRequiresNew() {
    return commandExecutorTxRequiresNew;
  }
  
  public ProcessEngineConfigurationImpl setCommandExecutorTxRequiresNew(CommandExecutor commandExecutorTxRequiresNew) {
    this.commandExecutorTxRequiresNew = commandExecutorTxRequiresNew;
    return this;
  }
  
  public RepositoryService getRepositoryService() {
    return repositoryService;
  }
  
  public ProcessEngineConfigurationImpl setRepositoryService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
    return this;
  }
  
  public RuntimeService getRuntimeService() {
    return runtimeService;
  }
  
  public ProcessEngineConfigurationImpl setRuntimeService(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
    return this;
  }
  
  public HistoryService getHistoryService() {
    return historyService;
  }
  
  public ProcessEngineConfigurationImpl setHistoryService(HistoryService historyService) {
    this.historyService = historyService;
    return this;
  }
  
  public IdentityService getIdentityService() {
    return identityService;
  }
  
  public ProcessEngineConfigurationImpl setIdentityService(IdentityService identityService) {
    this.identityService = identityService;
    return this;
  }
  
  public TaskService getTaskService() {
    return taskService;
  }
  
  public ProcessEngineConfigurationImpl setTaskService(TaskService taskService) {
    this.taskService = taskService;
    return this;
  }
  
  public FormService getFormService() {
    return formService;
  }
  
  public ProcessEngineConfigurationImpl setFormService(FormService formService) {
    this.formService = formService;
    return this;
  }
  
  public ManagementService getManagementService() {
    return managementService;
  }
  
  public ProcessEngineConfigurationImpl setManagementService(ManagementService managementService) {
    this.managementService = managementService;
    return this;
  }
  
  public Map<Class< ? >, SessionFactory> getSessionFactories() {
    return sessionFactories;
  }
  
  public ProcessEngineConfigurationImpl setSessionFactories(Map<Class< ? >, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }
  
  public List<Deployer> getDeployers() {
    return deployers;
  }
  
  public ProcessEngineConfigurationImpl setDeployers(List<Deployer> deployers) {
    this.deployers = deployers;
    return this;
  }
  
  public JobExecutor getJobExecutor() {
    return jobExecutor;
  }
  
  public ProcessEngineConfigurationImpl setJobExecutor(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
    return this;
  }
  
  public IdGenerator getIdGenerator() {
    return idGenerator;
  }
  
  public ProcessEngineConfigurationImpl setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }
  
  public String getWsSyncFactoryClassName() {
    return wsSyncFactoryClassName;
  }
  
  public ProcessEngineConfigurationImpl setWsSyncFactoryClassName(String wsSyncFactoryClassName) {
    this.wsSyncFactoryClassName = wsSyncFactoryClassName;
    return this;
  }
  
  public Map<String, FormEngine> getFormEngines() {
    return formEngines;
  }
  
  public ProcessEngineConfigurationImpl setFormEngines(Map<String, FormEngine> formEngines) {
    this.formEngines = formEngines;
    return this;
  }
  
  public FormTypes getFormTypes() {
    return formTypes;
  }
  
  public ProcessEngineConfigurationImpl setFormTypes(FormTypes formTypes) {
    this.formTypes = formTypes;
    return this;
  }
  
  public ScriptingEngines getScriptingEngines() {
    return scriptingEngines;
  }
  
  public ProcessEngineConfigurationImpl setScriptingEngines(ScriptingEngines scriptingEngines) {
    this.scriptingEngines = scriptingEngines;
    return this;
  }
  
  public VariableTypes getVariableTypes() {
    return variableTypes;
  }
  
  public ProcessEngineConfigurationImpl setVariableTypes(VariableTypes variableTypes) {
    this.variableTypes = variableTypes;
    return this;
  }
  
  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }
  
  public ProcessEngineConfigurationImpl setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
    return this;
  }
  
  public BusinessCalendarManager getBusinessCalendarManager() {
    return businessCalendarManager;
  }
  
  public ProcessEngineConfigurationImpl setBusinessCalendarManager(BusinessCalendarManager businessCalendarManager) {
    this.businessCalendarManager = businessCalendarManager;
    return this;
  }
  
  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }
  
  public ProcessEngineConfigurationImpl setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
    return this;
  }
  
  public TransactionContextFactory getTransactionContextFactory() {
    return transactionContextFactory;
  }
  
  public ProcessEngineConfigurationImpl setTransactionContextFactory(TransactionContextFactory transactionContextFactory) {
    this.transactionContextFactory = transactionContextFactory;
    return this;
  }

  
  public List<Deployer> getCustomPreDeployers() {
    return customPreDeployers;
  }

  
  public ProcessEngineConfigurationImpl setCustomPreDeployers(List<Deployer> customPreDeployers) {
    this.customPreDeployers = customPreDeployers;
    return this;
  }

  
  public List<Deployer> getCustomPostDeployers() {
    return customPostDeployers;
  }

  
  public ProcessEngineConfigurationImpl setCustomPostDeployers(List<Deployer> customPostDeployers) {
    this.customPostDeployers = customPostDeployers;
    return this;
  }

  
  public Map<String, JobHandler> getJobHandlers() {
    return jobHandlers;
  }

  
  public ProcessEngineConfigurationImpl setJobHandlers(Map<String, JobHandler> jobHandlers) {
    this.jobHandlers = jobHandlers;
    return this;
  }

  
  public SqlSessionFactory getSqlSessionFactory() {
    return sqlSessionFactory;
  }

  
  public ProcessEngineConfigurationImpl setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  
  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

  public ProcessEngineConfigurationImpl setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    return this;
  }
  
  public TransactionFactory getTransactionFactory() {
    return transactionFactory;
  }

  public ProcessEngineConfigurationImpl setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public List<SessionFactory> getCustomSessionFactories() {
    return customSessionFactories;
  }
  
  public ProcessEngineConfigurationImpl setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }
  
  public List<JobHandler> getCustomJobHandlers() {
    return customJobHandlers;
  }
  
  public ProcessEngineConfigurationImpl setCustomJobHandlers(List<JobHandler> customJobHandlers) {
    this.customJobHandlers = customJobHandlers;
    return this;
  }
  
  public List<FormEngine> getCustomFormEngines() {
    return customFormEngines;
  }
  
  public ProcessEngineConfigurationImpl setCustomFormEngines(List<FormEngine> customFormEngines) {
    this.customFormEngines = customFormEngines;
    return this;
  }

  public List<AbstractFormType> getCustomFormTypes() {
    return customFormTypes;
  }

  
  public ProcessEngineConfigurationImpl setCustomFormTypes(List<AbstractFormType> customFormTypes) {
    this.customFormTypes = customFormTypes;
    return this;
  }

  
  public List<String> getCustomScriptingEngineClasses() {
    return customScriptingEngineClasses;
  }

  
  public ProcessEngineConfigurationImpl setCustomScriptingEngineClasses(List<String> customScriptingEngineClasses) {
    this.customScriptingEngineClasses = customScriptingEngineClasses;
    return this;
  }

  public List<VariableType> getCustomPreVariableTypes() {
    return customPreVariableTypes;
  }

  
  public ProcessEngineConfigurationImpl setCustomPreVariableTypes(List<VariableType> customPreVariableTypes) {
    this.customPreVariableTypes = customPreVariableTypes;
    return this;
  }

  
  public List<VariableType> getCustomPostVariableTypes() {
    return customPostVariableTypes;
  }

  
  public ProcessEngineConfigurationImpl setCustomPostVariableTypes(List<VariableType> customPostVariableTypes) {
    this.customPostVariableTypes = customPostVariableTypes;
    return this;
  }

  public List<BpmnParseHandler> getPreBpmnParseHandlers() {
    return preBpmnParseHandlers;
  }
  
  public void setPreBpmnParseHandlers(List<BpmnParseHandler> preBpmnParseHandlers) {
    this.preBpmnParseHandlers = preBpmnParseHandlers;
  }
  
  public List<BpmnParseHandler> getCustomDefaultBpmnParseHandlers() {
    return customDefaultBpmnParseHandlers;
  }
  
  public void setCustomDefaultBpmnParseHandlers(List<BpmnParseHandler> customDefaultBpmnParseHandlers) {
    this.customDefaultBpmnParseHandlers = customDefaultBpmnParseHandlers;
  }

  public List<BpmnParseHandler> getPostBpmnParseHandlers() {
    return postBpmnParseHandlers;
  }

  public void setPostBpmnParseHandlers(List<BpmnParseHandler> postBpmnParseHandlers) {
    this.postBpmnParseHandlers = postBpmnParseHandlers;
  }

  public ActivityBehaviorFactory getActivityBehaviorFactory() {
    return activityBehaviorFactory;
  }
  
  public void setActivityBehaviorFactory(ActivityBehaviorFactory activityBehaviorFactory) {
    this.activityBehaviorFactory = activityBehaviorFactory;
  }
  
  public ListenerFactory getListenerFactory() {
    return listenerFactory;
  }

  public void setListenerFactory(ListenerFactory listenerFactory) {
    this.listenerFactory = listenerFactory;
  }
  
  public BpmnParseFactory getBpmnParseFactory() {
    return bpmnParseFactory;
  }
  
  public void setBpmnParseFactory(BpmnParseFactory bpmnParseFactory) {
    this.bpmnParseFactory = bpmnParseFactory;
  }

  public Map<Object, Object> getBeans() {
    return beans;
  }

  public void setBeans(Map<Object, Object> beans) {
    this.beans = beans;
  }

  @Override
  public ProcessEngineConfigurationImpl setClassLoader(ClassLoader classLoader) {
    super.setClassLoader(classLoader);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setDatabaseType(String databaseType) {
    super.setDatabaseType(databaseType);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setDataSource(DataSource dataSource) {
    super.setDataSource(dataSource);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
    super.setDatabaseSchemaUpdate(databaseSchemaUpdate);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setHistory(String history) {
    super.setHistory(history);
    return this;
  }
  
  

  @Override
  public ProcessEngineConfigurationImpl setIdBlockSize(int idBlockSize) {
    super.setIdBlockSize(idBlockSize);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcDriver(String jdbcDriver) {
    super.setJdbcDriver(jdbcDriver);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcPassword(String jdbcPassword) {
    super.setJdbcPassword(jdbcPassword);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcUrl(String jdbcUrl) {
    super.setJdbcUrl(jdbcUrl);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcUsername(String jdbcUsername) {
    super.setJdbcUsername(jdbcUsername);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJobExecutorActivate(boolean jobExecutorActivate) {
    super.setJobExecutorActivate(jobExecutorActivate);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerDefaultFrom(String mailServerDefaultFrom) {
    super.setMailServerDefaultFrom(mailServerDefaultFrom);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerHost(String mailServerHost) {
    super.setMailServerHost(mailServerHost);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerPassword(String mailServerPassword) {
    super.setMailServerPassword(mailServerPassword);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerPort(int mailServerPort) {
    super.setMailServerPort(mailServerPort);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerUseSSL(boolean useSSL) {
	    super.setMailServerUseSSL(useSSL);
	    return this;
	  }
  
  @Override
  public ProcessEngineConfigurationImpl setMailServerUseTLS(boolean useTLS) {
    super.setMailServerUseTLS(useTLS);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerUsername(String mailServerUsername) {
    super.setMailServerUsername(mailServerUsername);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
    super.setJdbcMaxActiveConnections(jdbcMaxActiveConnections);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
    super.setJdbcMaxCheckoutTime(jdbcMaxCheckoutTime);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
    super.setJdbcMaxIdleConnections(jdbcMaxIdleConnections);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
    super.setJdbcMaxWaitTime(jdbcMaxWaitTime);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setTransactionsExternallyManaged(boolean transactionsExternallyManaged) {
    super.setTransactionsExternallyManaged(transactionsExternallyManaged);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJpaEntityManagerFactory(Object jpaEntityManagerFactory) {
    this.jpaEntityManagerFactory = jpaEntityManagerFactory;
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJpaHandleTransaction(boolean jpaHandleTransaction) {
    this.jpaHandleTransaction = jpaHandleTransaction;
    return this;
  }
  
  @Override
  public ProcessEngineConfigurationImpl setJpaCloseEntityManager(boolean jpaCloseEntityManager) {
    this.jpaCloseEntityManager = jpaCloseEntityManager;
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcPingEnabled(boolean jdbcPingEnabled) {
    this.jdbcPingEnabled = jdbcPingEnabled;
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcPingQuery(String jdbcPingQuery) {
    this.jdbcPingQuery = jdbcPingQuery;
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcPingConnectionNotUsedFor(int jdbcPingNotUsedFor) {
    this.jdbcPingConnectionNotUsedFor = jdbcPingNotUsedFor;
    return this;
  }
  
  @Override
  public ProcessEngineConfigurationImpl setDefaultCamelContext(String defaultCamelContext) {
    super.defaultCamelContext = defaultCamelContext;
    return this;
  }

  public boolean isDbIdentityUsed() {
    return isDbIdentityUsed;
  }

  
  public void setDbIdentityUsed(boolean isDbIdentityUsed) {
    this.isDbIdentityUsed = isDbIdentityUsed;
  }

  
  public boolean isDbHistoryUsed() {
    return isDbHistoryUsed;
  }
  
  public void setDbHistoryUsed(boolean isDbHistoryUsed) {
    this.isDbHistoryUsed = isDbHistoryUsed;
  }
  
  public List<ResolverFactory> getResolverFactories() {
    return resolverFactories;
  }
  
  public void setResolverFactories(List<ResolverFactory> resolverFactories) {
    this.resolverFactories = resolverFactories;
  }

  public DeploymentManager getDeploymentManager() {
    return deploymentManager;
  }
  
  public void setDeploymentManager(DeploymentManager deploymentManager) {
    this.deploymentManager = deploymentManager;
  }
    
  public ProcessEngineConfigurationImpl setDelegateInterceptor(DelegateInterceptor delegateInterceptor) {
    this.delegateInterceptor = delegateInterceptor;
    return this;
  }
    
  public DelegateInterceptor getDelegateInterceptor() {
    return delegateInterceptor;
  }
    
  public RejectedJobsHandler getCustomRejectedJobsHandler() {
    return customRejectedJobsHandler;
  }
    
  public ProcessEngineConfigurationImpl setCustomRejectedJobsHandler(RejectedJobsHandler customRejectedJobsHandler) {
    this.customRejectedJobsHandler = customRejectedJobsHandler;
    return this;
  }

  public EventHandler getEventHandler(String eventType) {
    return eventHandlers.get(eventType);
  }
  
  public void setEventHandlers(Map<String, EventHandler> eventHandlers) {
    this.eventHandlers = eventHandlers;
  }
    
  public Map<String, EventHandler> getEventHandlers() {
    return eventHandlers;
  }
    
  public List<EventHandler> getCustomEventHandlers() {
    return customEventHandlers;
  }
    
  public void setCustomEventHandlers(List<EventHandler> customEventHandlers) {
    this.customEventHandlers = customEventHandlers;
  }
  
  public FailedJobCommandFactory getFailedJobCommandFactory() {
    return failedJobCommandFactory;
  }
  
  public ProcessEngineConfigurationImpl setFailedJobCommandFactory(FailedJobCommandFactory failedJobCommandFactory) {
    this.failedJobCommandFactory = failedJobCommandFactory;
    return this;
  }
  
  /**
   * Allows configuring a database table prefix which is used for all runtime operations of the process engine.
   * For example, if you specify a prefix named 'PRE1.', activiti will query for executions in a table named
   * 'PRE1.ACT_RU_EXECUTION_'. 
   * 
   * <p />
   * <strong>NOTE: the prefix is not respected by automatic database schema management. If you use 
   * {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_CREATE_DROP} 
   * or {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_TRUE}, activiti will create the database tables 
   * using the default names, regardless of the prefix configured here.</strong>  
   * 
   * @since 5.9
   */
  public ProcessEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
    return this;
  }
    
  public String getDatabaseTablePrefix() {
    return databaseTablePrefix;
  }

  public boolean isCreateDiagramOnDeploy() {
    return isCreateDiagramOnDeploy;
  }

  public ProcessEngineConfiguration setCreateDiagramOnDeploy(boolean createDiagramOnDeploy) {
    this.isCreateDiagramOnDeploy = createDiagramOnDeploy;
    return this;
  }
  
  public String getDatabaseSchema() {
    return databaseSchema;
  }
  
  public void setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
  }

  public DataSource getIdGeneratorDataSource() {
    return idGeneratorDataSource;
  }
  
  public void setIdGeneratorDataSource(DataSource idGeneratorDataSource) {
    this.idGeneratorDataSource = idGeneratorDataSource;
  }
  
  public String getIdGeneratorDataSourceJndiName() {
    return idGeneratorDataSourceJndiName;
  }

  public void setIdGeneratorDataSourceJndiName(String idGeneratorDataSourceJndiName) {
    this.idGeneratorDataSourceJndiName = idGeneratorDataSourceJndiName;
  }

  public int getBatchSizeProcessInstances() {
    return batchSizeProcessInstances;
  }

  public void setBatchSizeProcessInstances(int batchSizeProcessInstances) {
    this.batchSizeProcessInstances = batchSizeProcessInstances;
  }
  
  public int getBatchSizeTasks() {
    return batchSizeTasks;
  }
  
  public void setBatchSizeTasks(int batchSizeTasks) {
    this.batchSizeTasks = batchSizeTasks;
  }
  
  public int getProcessDefinitionCacheLimit() {
    return processDefinitionCacheLimit;
  }

  public void setProcessDefinitionCacheLimit(int processDefinitionCacheLimit) {
    this.processDefinitionCacheLimit = processDefinitionCacheLimit;
  }
  
  public DeploymentCache<ProcessDefinitionEntity> getProcessDefinitionCache() {
    return processDefinitionCache;
  }
  
  public void setProcessDefinitionCache(DeploymentCache<ProcessDefinitionEntity> processDefinitionCache) {
    this.processDefinitionCache = processDefinitionCache;
  }

  public int getKnowledgeBaseCacheLimit() {
    return knowledgeBaseCacheLimit;
  }

  public void setKnowledgeBaseCacheLimit(int knowledgeBaseCacheLimit) {
    this.knowledgeBaseCacheLimit = knowledgeBaseCacheLimit;
  }
  
  public DeploymentCache<Object> getKnowledgeBaseCache() {
    return knowledgeBaseCache;
  }
  
  public void setKnowledgeBaseCache(DeploymentCache<Object> knowledgeBaseCache) {
    this.knowledgeBaseCache = knowledgeBaseCache;
  }

  public boolean isEnableSafeBpmnXml() {
    return enableSafeBpmnXml;
  }

  public void setEnableSafeBpmnXml(boolean enableSafeBpmnXml) {
    this.enableSafeBpmnXml = enableSafeBpmnXml;
  }

	public UserInfoMap getUserInfoMap() {
		// TODO Auto-generated method stub
		return userInfoMap;
	}

	public void setUserInfoMap(UserInfoMap userInfoMap) {
		this.userInfoMap = userInfoMap;
	}

	public InstanceUpgrade getInstanceUpgrade() {
		return instanceUpgrade;
	}
  
}