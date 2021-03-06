package games.hebele.football.screens;

import java.util.ArrayList;
import java.util.Iterator;

import games.hebele.football.Variables;
import games.hebele.football.helpers.Assets;
import games.hebele.football.helpers.ContactHelper;
import games.hebele.football.helpers.GameController;
import games.hebele.football.helpers.GameEvent;
import games.hebele.football.helpers.GameEventManager;
import games.hebele.football.helpers.InputHandler;
import games.hebele.football.objects.Actress;
import games.hebele.football.objects.Ball;
import games.hebele.football.objects.Player;
import games.hebele.football.objects.enemies.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class PlayScreen implements Screen {

	private World world;
	private Body groundBody, ballBody;
	private GameEventManager eventManager;
	
	private TextureAtlas textureAtlas;
	private TextureRegion texturebg, textureGround;
	
	private Player player;
	
	private Stage stage;
	private ExtendViewport viewport;
	private OrthographicCamera camera;
	private TiledMap map;
	private OrthogonalTiledMapRenderer mapRenderer;
	
	private ShapeRenderer shapeRenderer;
	private SpriteBatch spriteBatch;
	
	private InputMultiplexer inputMultiplexer;
	private InputHandler inputHandler;
	private HUD hud;
	
	private Box2DDebugRenderer debugRenderer;
	
	private float Virtual_Width, Virtual_Height;
	
	private float runTime;
		
	private Array<Actress> actresses = new Array<Actress>();
	
	
	@Override
	public void show() {
		
		Virtual_Width=Variables.TILE_SIZE*Variables.TILES_PER_SCREENWIDTH/Variables.PIXEL_TO_METER;
		Virtual_Height=Virtual_Width*Gdx.graphics.getHeight()/Gdx.graphics.getWidth();
		
		Variables.Virtual_Ratio = Variables.VIRTUAL_STAGE_WIDTH / Virtual_Width;
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, Virtual_Width, Virtual_Height);
		
		spriteBatch = new SpriteBatch();
		
		//stage = new Stage(Virtual_Width*Variables.PIXEL_TO_METER,Virtual_Height*Variables.PIXEL_TO_METER,true,spriteBatch);

		viewport = new ExtendViewport(Variables.VIRTUAL_STAGE_WIDTH, Variables.VIRTUAL_STAGE_HEIGHT);
		stage = new Stage(viewport, spriteBatch);
		//stage.getViewport().update((int), (int)Variables.VIRTUAL_STAGE_HEIGHT);

		
		map = new TmxMapLoader().load("maps/map1.tmx");
		mapRenderer = new OrthogonalTiledMapRenderer(map,1/Variables.PIXEL_TO_METER,spriteBatch);
		shapeRenderer = new ShapeRenderer();
		
		world = new World(new Vector2(0,-9.81f),true);
		eventManager = new GameEventManager();
		
		debugRenderer = new Box2DDebugRenderer();
		

		Ball playerBall = new Ball(world);
		actresses.add(playerBall);
		player = new Player(world,playerBall,eventManager);
		actresses.add(player);

		setTiledMapShapes();


		inputHandler = new InputHandler(player);
		//Gdx.input.setInputProcessor(inputHandler);
		//Gdx.input.setInputProcessor(stage);
		
		inputMultiplexer = new InputMultiplexer(stage, inputHandler);
		Gdx.input.setInputProcessor(inputMultiplexer);
		
		ContactHelper contactHelper = new ContactHelper(world, player, ballBody);
		//world.setContactFilter(contactHelper);
		world.setContactListener(contactHelper);
		
		
		hud = new HUD(stage, player);
		

		//LOAD ASSETS-----------------------------
		textureAtlas = Assets.manager.get(Assets.footballPack, TextureAtlas.class);
		//BACKGROUND IMAGE
		texturebg = textureAtlas.findRegion("bg1");
		textureGround = textureAtlas.findRegion("ground2");
	}
	

	
	public void setTiledMapShapes(){
		
		BodyDef bodyDef = new BodyDef();
		FixtureDef fixtureDef = new FixtureDef();
		
		//BOUNDRIES OF MAP
		//CREATE DYNAMICALLY
		float[] vertices;
		Vector2[] groundVertices=null;
		
		int tileNumX = map.getProperties().get("width", Integer.class);
		
		//TOP CORNERS Y VALUE ARE DOUBLED TO SET THE CEILING HIGHER THAN THE TILES
		int tileNumY = map.getProperties().get("height", Integer.class) * 2;

		groundVertices = new Vector2[5];
		
		//GET 4 CORNERS OF THE WORLD
		groundVertices[0]=new Vector2(0,0);
		groundVertices[1]=new Vector2(0,tileNumY*Variables.TILE_SIZE/Variables.PIXEL_TO_METER);
		groundVertices[2]=new Vector2(tileNumX*Variables.TILE_SIZE/Variables.PIXEL_TO_METER,tileNumY*Variables.TILE_SIZE/Variables.PIXEL_TO_METER);
		groundVertices[3]=new Vector2(tileNumX*Variables.TILE_SIZE/Variables.PIXEL_TO_METER,0);
		groundVertices[4]=new Vector2(0,0);
		
		//ground shape
		bodyDef.type=BodyType.StaticBody;
		bodyDef.position.set(0,0);
		
		ChainShape groundShape = new ChainShape();
		
		groundShape.createChain(groundVertices);
		
		fixtureDef.shape=groundShape;
		fixtureDef.friction=0.1f;
		fixtureDef.restitution=0;
		fixtureDef.filter.maskBits &= ~Variables.CAGE_CATEGOTY;
		
		groundBody = world.createBody(bodyDef);
		groundBody.createFixture(fixtureDef).setUserData("ground");
		groundShape.dispose();
		//---------------------BOUNDRIES OF MAP
		
		
		
		//STEPS
		for(MapObject object : map.getLayers().get("objects").getObjects()){
			if(object instanceof PolylineMapObject){
				vertices = ((PolylineMapObject) object).getPolyline().getTransformedVertices();
				groundVertices = new Vector2[vertices.length/2];
				
				for(int i=0; i<vertices.length;){
					  groundVertices[i/2]=new Vector2(vertices[i]/Variables.PIXEL_TO_METER,vertices[i+1]/Variables.PIXEL_TO_METER);
					  i=i+2;
				}
				
				
				//ground shape
				bodyDef.type=BodyType.StaticBody;
				bodyDef.position.set(0,0);
				
				groundShape = new ChainShape();
				groundShape.createChain(groundVertices);
				
				fixtureDef.shape=groundShape;
				fixtureDef.friction=0.1f;
				fixtureDef.restitution=0;
				fixtureDef.filter.maskBits &= ~Variables.CAGE_CATEGOTY;
				
				groundBody = world.createBody(bodyDef);
				groundBody.createFixture(fixtureDef).setUserData("ground");
				groundShape.dispose();
				
			}
		}
		//------------STEPS
		
		
		//ENEMIES
		for(MapObject object : map.getLayers().get("enemies").getObjects()){
			if(object instanceof EllipseMapObject){
				
				
				float posX=((EllipseMapObject)object).getEllipse().x/Variables.PIXEL_TO_METER;
				float posY=((EllipseMapObject)object).getEllipse().y/Variables.PIXEL_TO_METER;
				
				Enemy e=null;
				String enemyType = object.getName();
				
				//System.out.println(enemyType);
				
				if(enemyType.equals("Kit")) e = new Kit(world, stage, player, posX, posY);
				else if(enemyType.equals("Droid")) e = new Droid(world, stage, player, posX, posY);
				else if(enemyType.equals("Gnu")) e = new Gnu(world, stage, player, posX, posY);
				else  e = new Wilber(world, stage, player, posX, posY);
				
				 
				actresses.add(e);
				
				//System.out.println(posX+" , "+posY);
			}
		}
		//------------ENEMIES
		
		
	}
	
    public void sweepDeadBodies() {
    	Iterator<Actress> iter = actresses.iterator();
    	while (iter.hasNext()) {
    		Actress a = iter.next();
    		if(a.isDead()){
				 a.destroy();
				 iter.remove();
			}
    	}
    }
    
    
    //GAME IS RUNNING - UPDATE DATA - STEP
    public void updateGameData(float delta){
		runTime+=delta;
		
		camera.position.x = player.getBody().getWorldCenter().x;
		camera.position.y = player.getBody().getWorldCenter().y;
		camera.update();

		sweepDeadBodies();
		
		world.step(1/60f, 8, 3);
		ArrayList<GameEvent> events = eventManager.getAndClean();
		for (Actress a : actresses) {
			a.step(delta,events);
		}
    }
    //------------------------------------------------
    
    

    
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//IOS VIEWPORT FIX
		Gdx.gl.glViewport(viewport.getViewportX(), viewport.getViewportY(), viewport.getViewportWidth(), viewport.getViewportHeight()); 
			
		//UPDATE DATAS WHEN GAME IS RUNNING
		//SHOW GAME OVER OR GAME WON 
		if(GameController.isGameRunning()) updateGameData(delta);
		else if(GameController.isGameOver()) hud.showGameOver();
		else if(GameController.isGameWon()) hud.showGameWon();
		//------------------------------------------------
		
		//DRAW BACKGROUND
		spriteBatch.begin();
		spriteBatch.draw(texturebg, 0, 0, stage.getWidth(), stage.getHeight());
		spriteBatch.end();
		
		//DRAW GROUND
		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();
		spriteBatch.draw(textureGround, camera.position.x - Virtual_Width/2, -Virtual_Height*0.5f, Virtual_Width, Virtual_Height*0.6f);
		spriteBatch.end();
		

		//DRAW TILEMAP
		mapRenderer.setView(camera);
		mapRenderer.render();
		

		spriteBatch.begin();
		//DRAW PLAYER
		
		for (Actress a : actresses) {
			a.draw(spriteBatch);
		}
		
		spriteBatch.end();
		
		//DRAW BOX2D DEBUG OBJECTS
		debugRenderer.render(world, camera.combined);
		
		//DRAW STAGE - HUD
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
		actresses.clear();
		map.dispose();
		mapRenderer.dispose();
		spriteBatch.dispose();
		shapeRenderer.dispose();
		world.dispose();
		debugRenderer.dispose();
		stage.dispose();
	}

}
