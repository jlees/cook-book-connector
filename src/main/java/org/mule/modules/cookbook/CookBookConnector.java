package org.mule.modules.cookbook;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.MetaDataScope;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.ReconnectOn;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceStrategy;
import org.mule.api.annotations.lifecycle.OnException;
import org.mule.api.annotations.oauth.OAuthProtected;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.MetaDataKeyParam;
import org.mule.api.annotations.param.MetaDataKeyParamAffectsType;
import org.mule.api.annotations.param.RefOnly;
import org.mule.api.callback.SourceCallback;
import org.mule.modules.cookbook.config.OAuthConfig;
import org.mule.modules.cookbook.datasense.DataSenseResolver;
import org.mule.modules.cookbook.handler.CookBookHandler;

import com.cookbook.tutorial.client.ICookbookCallback;
import com.cookbook.tutorial.service.CookBookEntity;
import com.cookbook.tutorial.service.Ingredient;
import com.cookbook.tutorial.service.InvalidEntityException;
import com.cookbook.tutorial.service.InvalidTokenException;
import com.cookbook.tutorial.service.NoSuchEntityException;
import com.cookbook.tutorial.service.Recipe;
import com.cookbook.tutorial.service.SessionExpiredException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Anypoint Connector
 *
 * @author MuleSoft, Inc.
 */
@Connector(name = "cookbook", friendlyName = "Cookbook")
@MetaDataScope(DataSenseResolver.class)
public class CookBookConnector {

    @Config
    OAuthConfig config;

    /**
     * Returns the list of recently added recipes
     *
     * {@sample.xml ../../../doc/cook-book-connector.xml.sample cook-book:getRecentlyAdded}
     *
     * @return A list of the recently added recipes
     */
    @OAuthProtected
    @Processor
    public List<Recipe> getRecentlyAdded() {
        return config.getClient().getRecentlyAdded();
    }

    /**
     * Description for getRecentlyAddedSource
     *
     * {@sample.xml ../../../doc/cook-book-connector.xml.sample cook-book:getRecentlyAddedSource}
     *
     * @param callback
     *            The callback that will hook the result into mule event.
     * @throws Exception
     *             When the source fails.
     */
    @OAuthProtected
    @Source(sourceStrategy = SourceStrategy.POLLING, pollingPeriod = 10000)
    public void getRecentlyAddedSource(final SourceCallback callback) throws Exception {

        if (this.getConfig().getClient() != null) {
            // Every 5 seconds our callback will be executed
            this.getConfig().getClient().getRecentlyAdded(new ICookbookCallback() {

                @Override
                public void execute(List<Recipe> recipes) throws Exception {
                    callback.process(recipes);
                }
            });

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    /**
     * Description for create
     *
     * {@sample.xml ../../../doc/cook-book-connector.xml.sample cook-book:create}
     *
     * @param entity
     *            Ingredient to be created
     * @return return Ingredient with Id from the system.
     *
     * @throws InvalidTokenException
     * @throws SessionExpiredException
     * @throws InvalidEntityException
     */
    @SuppressWarnings("unchecked")
    @OAuthProtected
    @Processor
    @OnException(handler = CookBookHandler.class)
    @ReconnectOn(exceptions = { SessionExpiredException.class })
    public Map<String, Object> create(@MetaDataKeyParam(affects = MetaDataKeyParamAffectsType.BOTH) String type, @Default("#[payload]") @RefOnly Map<String, Object> entity)
            throws InvalidEntityException, SessionExpiredException {
        ObjectMapper m = new ObjectMapper();
        CookBookEntity input = null;
        if (type.contains("com.cookbook.tutorial.service.Recipe")) {
            input = m.convertValue(entity, Recipe.class);
        } else if (type.contains("com.cookbook.tutorial.service.Ingredient")) {
            input = m.convertValue(entity, Ingredient.class);
        } else {
            throw new InvalidEntityException("Don't know how to handle type:" + type);
        }
        return m.convertValue(this.getConfig().getClient().create(input), Map.class);
    }

    /**
     * Description for update
     *
     * {@sample.xml ../../../doc/cook-book-connector.xml.sample cook-book:update}
     *
     * @param entity
     *            Ingredient to be updated
     * @return return Ingredient with Id from the system.
     *
     * @throws SessionExpiredException
     * @throws InvalidEntityException
     * @throws NoSuchEntityException
     */
    @SuppressWarnings("unchecked")
    @OAuthProtected
    @Processor
    @OnException(handler = CookBookHandler.class)
    @ReconnectOn(exceptions = { SessionExpiredException.class })
    public Map<String, Object> update(@MetaDataKeyParam(affects = MetaDataKeyParamAffectsType.BOTH) String type, @Default("#[payload]") @RefOnly Map<String, Object> entity)
            throws InvalidEntityException, SessionExpiredException, NoSuchEntityException {
        ObjectMapper m = new ObjectMapper();
        CookBookEntity input = null;
        if (type.contains("com.cookbook.tutorial.service.Recipe")) {
            input = m.convertValue(entity, Recipe.class);
        } else if (type.contains("com.cookbook.tutorial.service.Ingredient")) {
            input = m.convertValue(entity, Ingredient.class);
        } else {
            throw new InvalidEntityException("Don't know how to handle type:" + type);
        }
        return m.convertValue(this.getConfig().getClient().update(input), Map.class);
    }

    /**
     * Description for get
     *
     * {@sample.xml ../../../doc/cook-book-connector.xml.sample cook-book:get}
     *
     * @param id
     *            Id of the entity to retrieve
     * @return return Ingredient with Id from the system.
     *
     * @throws SessionExpiredException
     * @throws InvalidEntityException
     * @throws NoSuchEntityException
     * @throws IOException 
     * @throws JsonProcessingException 
     */
    @SuppressWarnings("unchecked")
    @OAuthProtected
    @Processor
    @OnException(handler = CookBookHandler.class)
    @ReconnectOn(exceptions = { SessionExpiredException.class })
    public Map<String, Object> get(@MetaDataKeyParam(affects = MetaDataKeyParamAffectsType.OUTPUT) String type, @Default("1") Integer id) throws InvalidEntityException,
            SessionExpiredException, NoSuchEntityException, JsonProcessingException, IOException {
    	
    	Client client = ClientBuilder.newClient();
    	WebTarget webTarget = client.target(this.config.getAddress()); // 
    	WebTarget ingredientTarget = webTarget.path("ingredient").path("1");  
    	WebTarget helloworldWebTargetWithQueryParam =
    			ingredientTarget.queryParam("access_token", this.config.getAccessToken()); // 

    	Invocation.Builder invocationBuilder =
    	        helloworldWebTargetWithQueryParam.request(MediaType.APPLICATION_JSON_TYPE); // 

    	Response response = invocationBuilder.get(); // 
    	System.out.println(response.getStatus());
    	
    	String jsonString = response.readEntity(String.class);
    	System.out.println(jsonString);    	

    	ObjectMapper objectMapper = new ObjectMapper();
    	JsonNode json = objectMapper.readTree(jsonString);
    	return objectMapper.convertValue(json, Map.class);
    }

    /**
//     * Description for get
//     *
//     * {@sample.xml ../../../doc/cook-book-connector.xml.sample cook-book:get}
//     *
//     * @param id
//     *            Id of the entity to retrieve
//     * @return return Ingredient with Id from the system.
//     *
//     * @throws SessionExpiredException
//     * @throws NoSuchEntityException
//     */
//    @OAuthProtected
//    @Processor
//    @OnException(handler = CookBookHandler.class)
//    @ReconnectOn(exceptions = { SessionExpiredException.class })
//    public void delete(@Default("1") Integer id) throws NoSuchEntityException, SessionExpiredException {
//        this.getConfig().getClient().delete(id);
//    }
//
//    /**
//     * Description for queryPaginated
//     *
//     * {@sample.xml ../../../doc/cook-book-connector.xml.sample cook-book:query-paginated}
//     *
//     *  @param query The query
//     *  @param pagingConfiguration the paging configuration
//     *  @return return comment
//     */
//    @OAuthProtected
//    @Processor
//    @ReconnectOn(exceptions = { SessionExpiredException.class })
//    @Paged                                                                                       // <1>
//    public ProviderAwarePagingDelegate<Map<String, Object>, CookBookConnector> queryPaginated(   // <2>
//            final String query, final PagingConfiguration pagingConfiguration)                   // <3>
//            throws SessionExpiredException {
//        return new CookBookPagingDelegate(query, pagingConfiguration.getFetchSize());
//    }
//
//    @Transformer(sourceTypes = { List.class })
//    public static List<Map<String, Object>> recipesToMaps(List<Recipe> list) {
//        ObjectMapper mapper = new ObjectMapper();
//        List<Map<String, Object>> result = mapper.convertValue(list, new TypeReference<List<Map<String, Object>>>() {
//        });
//        return result;
//    }
//
//    @Transformer(sourceTypes = { Recipe.class })
//    public static Map<String, Object> recipeToMap(Recipe recipe) {
//        ObjectMapper mapper = new ObjectMapper();
//        Map<String, Object> result = mapper.convertValue(recipe, new TypeReference<Map<String, Object>>() {
//        });
//        return result;
//    }

    public OAuthConfig getConfig() {
        return config;
    }

    public void setConfig(OAuthConfig config) {
        this.config = config;
    }
    
}