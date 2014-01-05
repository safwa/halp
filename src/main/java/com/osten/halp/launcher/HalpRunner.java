package com.osten.halp.launcher;

import com.osten.halp.model.io.DataCruncher;
import com.osten.halp.model.profiling.*;
import com.osten.halp.model.shared.DataModel;
import com.osten.halp.model.shared.DetectorModel;
import com.osten.halp.model.shared.FilterModel;
import com.osten.halp.model.shared.ProfileModel;
import com.osten.halp.model.statistics.Statistic;
import com.osten.halp.impl.io.CSVReader;
import com.osten.halp.impl.io.LongDataCruncher;
import com.osten.halp.impl.shared.LongDataModel;
import com.osten.halp.impl.shared.LongDetectorModel;
import com.osten.halp.impl.shared.LongFilterModel;
import com.osten.halp.impl.shared.LongProfileModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by server on 2013-12-18.
 */
public class HalpRunner
{
	//Holders
	private ProfileModel<Long> profileModel;
	private DataModel<Long> dataModel;
	private FilterModel<Long> filterModel;
	private DetectorModel<Long> detectorModel;

	//content
	private List<File> csvSources;
	private LinkedList<List<Statistic.DataType>> dataTypes;
	private List<ProfileModel.Profile> profiles;

	//Calculators
	DataCruncher<Long> cruncher;

	//Results
	List<Bottleneck> bottlenecks;

	//Settings
	private File outputFile;
	private File guessFile;
	private boolean usingOutputFile;
	private boolean usingGuessFile;
	private boolean guessUponEnd;

	public HalpRunner()
	{

		//Holders
		dataModel = new LongDataModel();
		filterModel = new LongFilterModel();
		detectorModel = new LongDetectorModel();
		profileModel = new LongProfileModel();

		//Content
		dataTypes = new LinkedList<List<Statistic.DataType>>();
		csvSources = new LinkedList<File>();
		profiles = new LinkedList<ProfileModel.Profile>();

		//Calculators
		cruncher = new LongDataCruncher();
		bottlenecks = new ArrayList<Bottleneck>();

		//Settings
		guessUponEnd = false;
		usingOutputFile = false;
		usingGuessFile = false;
	}

	public void parseArgs( String[] args )
	{

		Argument argument = Argument.Help;
		if(args.length == 0){
			System.out.println( "HalpRunner -F File1;File2;...  -T ResponseTime,Throughput,Rate,Memory,Swap,CPU,Unknown;... -P All,CPU,Memory,Network -G guessfile" );
			System.exit( -1 );
		}
		for( String arg : args )
		{
			if( arg.startsWith( "-" ) )
			{

				argument = getArgumentByStringArg( arg );

			}
			else
			{
				switch( argument )
				{
					case File:

						if( arg.contains( ";" ) )
						{
							String[] files = arg.split( ";" );

							for( String file : files )
							{
								csvSources.add( new File( file ) );
							}
						}
						else
						{
							csvSources.add( new File( arg ) );
						}

						break;
					case Profile:

						if( arg.contains( "," ) )
						{
							String[] profileList = arg.split( "," );
							for( String profile : profileList )
							{
								profiles.add( getProfileByString( profile ) );
							}
						}
						else
						{
							profiles.add( getProfileByString( arg ) );
						}

						break;
					case Types:

						if( arg.contains( ";" ) )
						{
							String[] typeByFile = arg.split( ";" );
							for( int i = 0; i < typeByFile.length; i++ )
							{
								String types = typeByFile[i];
								String[] dataType = types.split( "," );
								dataTypes.add( new ArrayList<Statistic.DataType>() );

								for( String type : dataType )
								{
									dataTypes.get( i ).add( getTypebyString( type ) );
								}
							}
						}
						else
						{

							String[] dataType = arg.split( "," );
							dataTypes.add( new ArrayList<Statistic.DataType>() );

							for( String type : dataType )
							{
								dataTypes.get( 0 ).add( getTypebyString( type ) );
							}

						}

						break;
					case Guess:
						usingGuessFile = true;
						guessFile = new File( arg );

						break;
					case Output:

						usingOutputFile = true;
						outputFile = new File( arg );
						break;

					//print stuff on usage
					default:
						System.out.println( "HalpRunner -F File1;File2;...  -T ResponseTime,Throughput,Rate,Memory,Swap,CPU,Unknown;... -P All,CPU,Memory,Network -G guessfile -O AbsoluteOutputFile" );
				}
			}
		}
	}

	private ProfileModel.Profile getProfileByString( String profile )
	{

		if( profile.equalsIgnoreCase( ProfileModel.Profile.ALL.toString() ) )
		{
			return ProfileModel.Profile.ALL;
		}
		else if( profile.equalsIgnoreCase( ProfileModel.Profile.Memory.toString() ) )
		{
			return ProfileModel.Profile.Memory;
		}
		else if( profile.equalsIgnoreCase( ProfileModel.Profile.CPU.toString() ) )
		{
			return ProfileModel.Profile.CPU;
		}
		else if( profile.equalsIgnoreCase( ProfileModel.Profile.Network.toString() ) )
		{
			return ProfileModel.Profile.Network;
		}
		else
		{
			return ProfileModel.Profile.Custom;
		}
	}

	private Statistic.DataType getTypebyString( String type )
	{
		if( type.equalsIgnoreCase( Statistic.DataType.ResponseTime.toString() ) )
		{
			return Statistic.DataType.ResponseTime;
		}
		else if( type.equalsIgnoreCase( Statistic.DataType.Throughput.toString() ) )
		{
			return Statistic.DataType.Throughput;
		}
		else if( type.equalsIgnoreCase( Statistic.DataType.Memory.toString() ) )
		{
			return Statistic.DataType.Memory;
		}
		else if( type.equalsIgnoreCase( Statistic.DataType.CPU.toString() ) )
		{
			return Statistic.DataType.CPU;
		}
		else if( type.equalsIgnoreCase( Statistic.DataType.Swap.toString() ) )
		{
			return Statistic.DataType.Swap;
		}
		else if( type.equalsIgnoreCase( Statistic.DataType.Rate.toString() ) )
		{
			return Statistic.DataType.Rate;
		}
		else if( type.equalsIgnoreCase( Statistic.DataType.Unknown.toString() ) )
		{
			return Statistic.DataType.Zero;
		}
		else
		{
			return Statistic.DataType.Unknown;
		}
	}

	private Argument getArgumentByStringArg( String arg )
	{
		if( arg.equalsIgnoreCase( "-F" ) )
		{
			return Argument.File;
		}
		else if( arg.equalsIgnoreCase( "-P" ) )
		{
			return Argument.Profile;
		}
		else if( arg.equalsIgnoreCase( "-T" ) )
		{
			return Argument.Types;
		}
		else if( arg.equalsIgnoreCase( "-G" ) )
		{
			return Argument.Guess;
		}
		else if( arg.equalsIgnoreCase( "-O" ) )
		{
			return Argument.Output;
		}
		else
		{
			return Argument.Help;
		}
	}

	private enum Argument
	{
		File, Profile, Types, Guess, Output, Help
	}

	public void start()
	{
		compileDataModel();
		profileModel.buildProfiles();
		assembleFiltersDetectorsAndPointsOfInterestByProfile();
	}

	public void findAndShowPointsOfInterest()
	{
		PointsOfInterest poi = profileModel.getPointsOfInterests();
		poi.setProfile( profileModel.getSelectedProfile() );
		System.out.println( "\nRunning " + poi.getProfile().toString() + "-profiling yielded the following points of interest:\n================================================ " );
		int i = 0;
		int accumulator = 0;
		for( Range range : poi.getPointOfInterest() )
		{
			if( poi.getRelevance( range ) != PointsOfInterest.Relevance.Irrelevant )
			{
				accumulator += range.getStop() - range.getStart();
			}
			System.out.println( poi.getRelevance( range ) + " PoI #" + ++i + "--> { " + range.getStart() + " --> " + range.getStop() + " }" );
		}
		double d = Math.round( ( accumulator * 100 ) / poi.getInvolvedStatistics().get( 0 ).size() );
		System.out.println( "\nBottleneck likelihood ==> { " + d + "% }" );
		System.out.println( "================================================" );

		if( usingGuessFile )
		{
			bottlenecks.add( new Bottleneck( poi.getProfile(), profileModel.getDescriptionByProfile( poi.getProfile() ), d ) );
		}
	}

	public void assembleFiltersDetectorsAndPointsOfInterestByProfile()
	{
		for( ProfileModel.Profile profile : profiles )
		{
			profileModel.selectProfile( profile );

			if( profile != ProfileModel.Profile.Custom )
			{
				profileModel.resetModel();
				filterModel.resetModel();
				detectorModel.resetModel();
			}

			for( Statistic<Long> statistic : dataModel.getData() )
			{
				AdaptiveFilter.FilterType filterType = profileModel.getFilterByDataType( statistic.getType() );
				ChangeDetector.DetectorType detectorType = profileModel.getDetectorByDataType( statistic.getType() );
				if( filterType != null && detectorType != null )
				{
					filterModel.createFilter( statistic.getName(), filterType );
					detectorModel.createDetector( statistic.getName(), detectorType );
				}
			}

			applyFiltersAndDetectors();
			generatePointsOfInterests();
			findAndShowPointsOfInterest();
		}

		Bottleneck candidate = new Bottleneck( ProfileModel.Profile.Baseline, "No profiling has been performed", 0d );
		if( usingGuessFile )
		{
			Collections.sort( bottlenecks );
			for( Bottleneck bn : bottlenecks )
			{
				if( candidate.getLikeliness() < bn.getLikeliness() && bn.getLikeliness() > 5 )
				{
					candidate = bn;
				}
			}

			if( candidate.getType() == ProfileModel.Profile.Baseline )
			{
				candidate.setLikeliness( 100 );
			}

			try
			{
				FileWriter bw = new FileWriter( guessFile, true );

				if( guessFile.length() == 0 )
				{
					guessFile.createNewFile();
					bw.write( "Timestamp,Actual,Guess,Correct,CPU,Memory,Network\n" );
					bw.flush();
				}

				DateFormat dateFormat = new SimpleDateFormat( "HH:mm:ss" );
				Date date = new Date();
				String timestamp = dateFormat.format( date );

				ProfileModel.Profile actual = getCorrectBottleneckByFilename( csvSources.get( 0 ).getName() );
				bw.append( timestamp + "," + actual + "," + candidate.getType() + "," + ( actual == candidate.getType() ) + "" );

				for( Bottleneck bn : bottlenecks )
				{
					bw.append( "," + bn.getLikeliness() );
				}
				bw.append( "\n" );
				bw.close();

			}
			catch( IOException e )
			{
				//Nothing

			}
		}

		System.out.println( "With confidence of " + candidate.getLikeliness() + "% it is most likely a " + candidate.getType().

				toString()

				+ "-bottleneck." );
	}

	private ProfileModel.Profile getCorrectBottleneckByFilename( String filename )
	{
		ProfileModel.Profile correct = ProfileModel.Profile.Baseline;
		for( String s : filename.split( "-" ) )
		{
			if( s.equalsIgnoreCase( "memory" ) )
			{
				correct = ProfileModel.Profile.Memory;
				break;
			}
			else if( s.equalsIgnoreCase( "baseline" ) )
			{
				correct = ProfileModel.Profile.Baseline;
				break;
			}
			else if( s.equalsIgnoreCase( "cpu" ) )
			{
				correct = ProfileModel.Profile.CPU;
				break;
			}
			else if( s.equalsIgnoreCase( "network" ) )
			{
				correct = ProfileModel.Profile.Network;
				break;
			}
			else
			{
				//Nothing.
			}
		}

		return correct;
	}

	private void applyFiltersAndDetectors()
	{
		for( Statistic<Long> statistic : dataModel.getData() )
		{
			for( AdaptiveFilter<Long> filter : filterModel.getFiltersByStatisticName( statistic.getName() ) )
			{
				filter.reset();
				filter.adapt( statistic );

				for( ChangeDetector<Long> detector : detectorModel.getDetectorsByStatisticName( statistic.getName() ) )
				{
					detector.detect( filter );
					detector.printDetections();
				}
				filter.printAggregatedData();
			}
		}
	}

	private void generatePointsOfInterests()
	{
		HashMap<Statistic<Long>, List<Detection<Long>>> statisticDetectionMap = new HashMap<Statistic<Long>, List<Detection<Long>>>();
		for( Statistic<Long> statistic : dataModel.getData() )
		{
			statisticDetectionMap.put( statistic, new ArrayList<Detection<Long>>() );
			for( ChangeDetector<Long> changeDetector : detectorModel.getDetectorsByStatisticName( statistic.getName() ) )
			{
				statisticDetectionMap.put( statistic, changeDetector.getDetections() );
			}
		}
		profileModel.generatePointsOfInterests( statisticDetectionMap );
	}

	public void compileDataModel()
	{
		try
		{
			for( File file : csvSources )
			{
				System.out.println( "Reading Data from " + file.getName() );
				CSVReader reader = new CSVReader( file );
				List<Statistic<Long>> statisticList = cruncher.crunch( reader );
				List<Statistic.DataType> types = dataTypes.pop();
				for( int i = 0; i < statisticList.size(); i++ )
				{
					statisticList.get( i ).setType( types.get( i ) );
				}
				dataModel.getData().addAll( statisticList );
			}
		}
		catch( IOException e )
		{
			System.out.println( "Cannot read file" );
		}
		System.out.println( "Finished compiling data" );
	}
}
