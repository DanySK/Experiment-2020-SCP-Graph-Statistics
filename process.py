import numpy as np
import xarray as xr
import re
from pathlib import Path
import collections

def distance(val, ref):
    return abs(ref - val)
vectDistance = np.vectorize(distance)

def cmap_xmap(function, cmap):
    """ Applies function, on the indices of colormap cmap. Beware, function
    should map the [0, 1] segment to itself, or you are in for surprises.

    See also cmap_xmap.
    """
    cdict = cmap._segmentdata
    function_to_map = lambda x : (function(x[0]), x[1], x[2])
    for key in ('red','green','blue'):
        cdict[key] = map(function_to_map, cdict[key])
#        cdict[key].sort()
#        assert (cdict[key][0]<0 or cdict[key][-1]>1), "Resulting indices extend out of the [0, 1] segment."
    return matplotlib.colors.LinearSegmentedColormap('colormap',cdict,1024)

def getClosest(sortedMatrix, column, val):
    while len(sortedMatrix) > 3:
        half = int(len(sortedMatrix) / 2)
        sortedMatrix = sortedMatrix[-half - 1:] if sortedMatrix[half, column] < val else sortedMatrix[: half + 1]
    if len(sortedMatrix) == 1:
        result = sortedMatrix[0].copy()
        result[column] = val
        return result
    else:
        safecopy = sortedMatrix.copy()
        safecopy[:, column] = vectDistance(safecopy[:, column], val)
        minidx = np.argmin(safecopy[:, column])
        safecopy = safecopy[minidx, :].A1
        safecopy[column] = val
        return safecopy

def convert(column, samples, matrix):
    return np.matrix([getClosest(matrix, column, t) for t in samples])

def valueOrEmptySet(k, d):
    return (d[k] if isinstance(d[k], set) else {d[k]}) if k in d else set()

def mergeDicts(d1, d2):
    """
    Creates a new dictionary whose keys are the union of the keys of two
    dictionaries, and whose values are the union of values.

    Parameters
    ----------
    d1: dict
        dictionary whose values are sets
    d2: dict
        dictionary whose values are sets

    Returns
    -------
    dict
        A dict whose keys are the union of the keys of two dictionaries,
    and whose values are the union of values

    """
    res = {}
    for k in d1.keys() | d2.keys():
        res[k] = valueOrEmptySet(k, d1) | valueOrEmptySet(k, d2)
    return res

def extractCoordinates(filename):
    """
    Scans the header of an Alchemist file in search of the variables.

    Parameters
    ----------
    filename : str
        path to the target file
    mergewith : dict
        a dictionary whose dimensions will be merged with the returned one

    Returns
    -------
    dict
        A dictionary whose keys are strings (coordinate name) and values are
        lists (set of variable values)

    """
    with open(filename, 'r') as file:
#        regex = re.compile(' (?P<varName>[a-zA-Z._-]+) = (?P<varValue>[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?),?')
        regex = r"(?P<varName>[a-zA-Z._-]+) = (?P<varValue>[^,]*),?"
        dataBegin = r"\d"
        is_float = r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?"
        for line in file:
            match = re.findall(regex, line)
            if match:
                return {
                    var : float(value) if re.match(is_float, value)
                        else bool(re.match(r".*?true.*?", value.lower())) if re.match(r".*?(true|false).*?", value.lower())
                        else value
                    for var, value in match
                }
            elif re.match(dataBegin, line[0]):
                return {}

def extractVariableNames(filename):
    """
    Gets the variable names from the Alchemist data files header.

    Parameters
    ----------
    filename : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    with open(filename, 'r') as file:
        dataBegin = re.compile('\d')
        lastHeaderLine = ''
        for line in file:
            if dataBegin.match(line[0]):
                break
            else:
                lastHeaderLine = line
        if lastHeaderLine:
            regex = re.compile(' (?P<varName>\S+)')
            return regex.findall(lastHeaderLine)
        return []

def openCsv(path):
    """
    Converts an Alchemist export file into a list of lists representing the matrix of values.

    Parameters
    ----------
    path : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    regex = re.compile('\d')
    with open(path, 'r') as file:
        lines = filter(lambda x: regex.match(x[0]), file.readlines())
        return [[float(x) for x in line.split()] for line in lines]

def beautifyValue(v):
    """
    Converts an object to a better version for printing, in particular:
        - if the object converts to float, then its float value is used
        - if the object can be rounded to int, then the int value is preferred

    Parameters
    ----------
    v : object
        the object to try to beautify

    Returns
    -------
    object or float or int
        the beautified value
    """
    try:
        v = float(v)
        if v.is_integer():
            return int(v)
        return v
    except:
        return v

if __name__ == '__main__':
    # CONFIGURE SCRIPT
    # Where to find Alchemist data files
    directory = 'data'
    # Where to save charts
    output_directory = 'charts'
    # How to name the summary of the processed data
    pickleOutput = 'data_summary'
    # Experiment prefixes: one per experiment (root of the file name)
    experiments = {
        'simulation': (None, None, 100),
        'converge': (None, None, 1000),
        'leaderelection': (None, None, 50),
    }
    floatPrecision = '{: 0.3f}'
    # time management
    timeColumnName = 'time'
    logarithmicTime = False
    # One or more variables are considered random and "flattened"
    seedVars = ['seed', 'longseed']
    # Label mapping
    class Measure:
        def __init__(self, description, unit = None):
            self.__description = description
            self.__unit = unit
        def description(self):
            return self.__description
        def unit(self):
            return '' if self.__unit is None else f'({self.__unit})'
        def derivative(self, new_description = None, new_unit = None):
            def cleanMathMode(s):
                return s[1:-1] if s[0] == '$' and s[-1] == '$' else s
            def deriveString(s):
                return r'$d ' + cleanMathMode(s) + r'/{dt}$'
            def deriveUnit(s):
                return f'${cleanMathMode(s)}' + '/{s}$' if s else None
            result = Measure(
                new_description if new_description else deriveString(self.__description),
                new_unit if new_unit else deriveUnit(self.__unit),
            )
            return result
        def __str__(self):
            return f'{self.description()} {self.unit()}'
    
    centrality_label = 'H_a'
    def expected(x):
        return r'\mathbf{E}[' + x + ']'
    def stdev_of(x):
        return r'\sigma{}[' + x + ']'
    def mse(x):
        return 'MSE[' + x + ']'
    def mae(x):
        return 'MAE[' + x + ']'
    def cardinality(x):
        return r'\|' + x + r'\|'

    cluster_intra_distance = r'\overline{IntraDist}'
    cluster_neighbor_distance = r'\overline{ClosestNeighDist}'
    cluster_count = r'\mathcal{C}'
    degree = '{degree}'
    closeness = '{closeness}'
    harmonic = '{harmonic}'
    pagerank = '{pagerank}'
    random = '{random}'
    labels = {
        'error-central[Sum]': Measure('optimal'),
        'error-extreme[Sum]': Measure('worst'),
        'error-random[Sum]': Measure('random'),
        'error-pageRank[Sum]': Measure('pagerank'),
        'error-harmonic[Sum]': Measure('harmonic'),
        'error-closeness[Sum]': Measure('closeness'),
        'error-degree[Sum]': Measure('degree'),
        'nodeCount': Measure(r'$n$', 'nodes'),
        'harmonicCentrality[Mean]': Measure(f'${expected("H")}$'),
        'meanNeighbors': Measure(f'${expected("N")}$', 'nodes'),
        'speed': Measure(r'$\|\vec{v}\|$', r'$m/s$'),
        'msqer@harmonicCentrality[Max]': Measure(r'$\max{(' + mse(centrality_label) + ')}$'),
        'msqer@harmonicCentrality[Min]': Measure(r'$\min{(' + mse(centrality_label) + ')}$'),
        'msqer@harmonicCentrality[Mean]': Measure(f'${mae(centrality_label)}$'),
        'msqer@harmonicCentrality[StandardDeviation]': Measure(f'${stdev_of(mse(centrality_label))}$'),
        'org:protelis:armonicCentralityHLL[Mean]': Measure(f'${expected(centrality_label)}$'),
        # Additional experiment
        'HopCountClusterIntraDistance[leader-closeness]': Measure(f'${cluster_intra_distance}_{closeness}$', 'hops'),
        'HopCountClusterIntraDistance[leader-degree]': Measure(f'${cluster_intra_distance}_{degree}$', 'hops'),
        'HopCountClusterIntraDistance[leader-harmonic]': Measure(f'${cluster_intra_distance}_{harmonic}$', 'hops'),
        'HopCountClusterIntraDistance[leader-pageRank]': Measure(f'${cluster_intra_distance}_{pagerank}$', 'hops'),
        'HopCountClusterIntraDistance[leader-random]': Measure(f'${cluster_intra_distance}_{random}$', 'hops'),
        'HopCountClusterClosestNeighborDistance[leader-closeness]': Measure(f'${cluster_neighbor_distance}_{closeness}$', 'hops'),
        'HopCountClusterClosestNeighborDistance[leader-degree]': Measure(f'${cluster_neighbor_distance}_{degree}$', 'hops'),
        'HopCountClusterClosestNeighborDistance[leader-harmonic]': Measure(f'${cluster_neighbor_distance}_{harmonic}$', 'hops'),
        'HopCountClusterClosestNeighborDistance[leader-pageRank]': Measure(f'${cluster_neighbor_distance}_{pagerank}$', 'hops'),
        'HopCountClusterClosestNeighborDistance[leader-random]': Measure(f'${cluster_count}_{random}$', 'hops'),
        'leader-closeness[CountDistinct]': Measure(f'${cluster_count}_{closeness}$', 'clusters'),
        'leader-degree[CountDistinct]': Measure(f'${cluster_count}_{degree}$', 'clusters'),
        'leader-harmonic[CountDistinct]': Measure(f'${cluster_count}_{harmonic}$', 'clusters'),
        'leader-pageRank[CountDistinct]': Measure(f'${cluster_count}_{pagerank}$', 'clusters'),
        'leader-random[CountDistinct]': Measure(f'${cluster_count}_{random}$', 'clusters'),
        'grain': Measure('cluster radius', 'hops'),
        'deploymentType': Measure(r'$\mathcal{D}$'),
    }
    print(labels['harmonicCentrality[Mean]'])
    print(labels['HopCountClusterClosestNeighborDistance[leader-random]'])

    def derivativeOrMeasure(variable_name):
        if variable_name.endswith('dt'):
            return labels.get(variable_name[:-2], Measure(variable_name)).derivative()
        return Measure(variable_name)
    def label_for(variable_name):
        return labels.get(variable_name, derivativeOrMeasure(variable_name)).description()
    def unit_for(variable_name):
        return str(labels.get(variable_name, derivativeOrMeasure(variable_name)))
    
    # Setup libraries
    np.set_printoptions(formatter={'float': floatPrecision.format})
    # Read the last time the data was processed, reprocess only if new data exists, otherwise just load
    import pickle
    import os
    newestFileTime = max(os.path.getmtime(directory + '/' + file) for file in os.listdir(directory))
    try:
        lastTimeProcessed = pickle.load(open('timeprocessed', 'rb'))
    except:
        lastTimeProcessed = -1
    shouldRecompute = not os.path.exists(".skip_data_process") and newestFileTime != lastTimeProcessed
    if not shouldRecompute:
        try:
            means = pickle.load(open(pickleOutput + '_mean', 'rb'))
            stdevs = pickle.load(open(pickleOutput + '_std', 'rb'))
            medians = pickle.load(open(pickleOutput + '_median', 'rb'))
        except:
            shouldRecompute = True
    if shouldRecompute:
        timefun = np.logspace if logarithmicTime else np.linspace
        means = {}
        stdevs = {}
        medians = {}
        for experiment, (minTime, maxTime, timeSamples) in experiments.items():
            # Collect all files for the experiment of interest
            import fnmatch
            allfiles = filter(lambda file: fnmatch.fnmatch(file, experiment + '_*.txt'), os.listdir(directory))
            allfiles = [directory + '/' + name for name in allfiles]
            allfiles.sort()
            # From the file name, extract the independent variables
            dimensions = {}
            for file in allfiles:
                dimensions = mergeDicts(dimensions, extractCoordinates(file))
            dimensions = {k: sorted(v) for k, v in dimensions.items()}
            # Add time to the independent variables
            dimensions[timeColumnName] = range(0, timeSamples)
            # Compute the matrix shape
            shape = tuple(len(v) for k, v in dimensions.items())
            # Prepare the Dataset
            dataset = xr.Dataset()
            for k, v in dimensions.items():
                dataset.coords[k] = v
            if len(allfiles) == 0:
                print("WARNING: No data for experiment " + experiment)
            else:
                varNames = extractVariableNames(allfiles[0])
                for v in varNames:
                    if v != timeColumnName:
                        novals = np.ndarray(shape)
                        novals.fill(float('nan'))
                        dataset[v] = (dimensions.keys(), novals)
                # Compute maximum and minimum time, create the resample
                timeColumn = varNames.index(timeColumnName)
                allData = { file: np.matrix(openCsv(file)) for file in allfiles }
                computeMin = minTime is None
                computeMax = maxTime is None
                if computeMax:
                    maxTime = float('-inf')
                    for data in allData.values():
                        maxTime = max(maxTime, data[-1, timeColumn])
                if computeMin:
                    minTime = float('inf')
                    for data in allData.values():
                        minTime = min(minTime, data[0, timeColumn])
                timeline = timefun(minTime, maxTime, timeSamples)
                # Resample
                for file in allData:
#                    print(file)
                    allData[file] = convert(timeColumn, timeline, allData[file])
                # Populate the dataset
                for file, data in allData.items():
                    dataset[timeColumnName] = timeline
                    for idx, v in enumerate(varNames):
                        if v != timeColumnName:
                            darray = dataset[v]
                            experimentVars = extractCoordinates(file)
                            darray.loc[experimentVars] = data[:, idx].A1
                # Fold the dataset along the seed variables, producing the mean and stdev datasets
                mergingVariables = [seed for seed in seedVars if seed in dataset.coords]
                # Deal with speciality of third experiment!!!
                if experiment == 'leaderelection':
                    dataset = dataset.fillna(0)
                means[experiment] = dataset.mean(dim = mergingVariables, skipna=True)
                stdevs[experiment] = dataset.std(dim = mergingVariables, skipna=True)
                medians[experiment] = dataset.median(dim = mergingVariables, skipna=True)
        # Save the datasets
        pickle.dump(means, open(pickleOutput + '_mean', 'wb'), protocol=-1)
        pickle.dump(stdevs, open(pickleOutput + '_std', 'wb'), protocol=-1)
        pickle.dump(stdevs, open(pickleOutput + '_median', 'wb'), protocol=-1)
        pickle.dump(newestFileTime, open('timeprocessed', 'wb'))

    # NORMALIZE
    norm_centrality_label = f'${expected("H")}$'
    for collection in [means, stdevs, medians]:
        collection['simulation'][norm_centrality_label] = collection['simulation']['msqer@harmonicCentrality[Mean]'] / (collection['simulation']['nodeCount'] - 1)

    # QUICK CHARTING

    import matplotlib
    import matplotlib.pyplot as plt
    import matplotlib.cm as cmx
    matplotlib.rcParams.update({'axes.titlesize': 12})
    matplotlib.rcParams.update({'axes.labelsize': 10})
    def make_line_chart(xdata, ydata, title = None, ylabel = None, xlabel = None, colors = None, linewidth = 1, errlinewidth = 0.5, figure_size = (6, 4)):
        fig = plt.figure(figsize = figure_size)
        ax = fig.add_subplot(1, 1, 1)
        ax.set_title(title)
        ax.set_xlabel(xlabel)
        ax.set_ylabel(ylabel)
#        ax.set_ylim(0)
#        ax.set_xlim(min(xdata), max(xdata))
        index = 0
        for (label, (data, error)) in ydata.items():
#            print(f'plotting {data}\nagainst {xdata}')
            lines = ax.plot(xdata, data, label=label, color=colors(index / (len(ydata) - 1)) if colors else None, linewidth=linewidth)
            index += 1
            if error is not None:
                last_color = lines[-1].get_color()
                ax.plot(xdata, data+error, label=None, color=last_color, linewidth=errlinewidth)
                ax.plot(xdata, data-error, label=None, color=last_color, linewidth=errlinewidth)
        return (fig, ax)
    def generate_all_charts(means, errors = None, basedir=''):
        viable_coords = { coord for coord in means.coords if means[coord].size > 1 }
        for comparison_variable in viable_coords - {timeColumnName}:
            mergeable_variables = viable_coords - {timeColumnName, comparison_variable}
            for current_coordinate in mergeable_variables:
                merge_variables = mergeable_variables - { current_coordinate }
                merge_data_view = means.mean(dim = merge_variables, skipna = True)
                merge_error_view = errors.mean(dim = merge_variables, skipna = True)
                for current_coordinate_value in merge_data_view[current_coordinate].values:
                    beautified_value = beautifyValue(current_coordinate_value)
                    for current_metric in merge_data_view.data_vars:
                        title = f'{label_for(current_metric)} for diverse {label_for(comparison_variable)} when {label_for(current_coordinate)}={beautified_value}'
                        print(title)
                        for withErrors in [True, False]:
                            fig, ax = make_line_chart(
                                title = title,
                                xdata = merge_data_view[timeColumnName],
                                xlabel = unit_for(timeColumnName),
                                ylabel = unit_for(current_metric),
                                ydata = {
                                    beautifyValue(label): (
                                        merge_data_view.sel(selector)[current_metric],
                                        merge_error_view.sel(selector)[current_metric] if withErrors else 0
                                    )
                                    for label in merge_data_view[comparison_variable].values
                                    for selector in [{comparison_variable: label, current_coordinate: current_coordinate_value}]
                                },
                            ) 
                            # logscale!!!
                            ax.set_xlim(min(merge_data_view[timeColumnName]), max(merge_data_view[timeColumnName]))
                            ax.legend()
                            fig.tight_layout()
                            by_time_output_directory = f'{output_directory}/{basedir}/{comparison_variable}'
                            Path(by_time_output_directory).mkdir(parents=True, exist_ok=True)
                            figname = f'{comparison_variable}_{current_metric}_{current_coordinate}_{beautified_value}{"_err" if withErrors else ""}'
                            for symbol in r".[]\/@:${}":
                                figname = figname.replace(symbol, '_')
                            fig.savefig(f'{by_time_output_directory}/{figname}.pdf')
                            ax.set_yscale('log')
                            fig.savefig(f'{by_time_output_directory}/{figname}_log.pdf')
                            plt.close(fig)
    for experiment in experiments:
        current_experiment_means = means[experiment]
        current_experiment_errors = stdevs[experiment]
        generate_all_charts(current_experiment_means, current_experiment_errors, basedir = f'{experiment}/all')
    
# Custom charting

    converge_data = means['converge']
    converge_error = stdevs['converge']
    timeline = converge_data['time']
    for diameter in converge_data['diameter']:
        diameter_value = float(diameter)
        nice_value = beautifyValue(diameter)
        fig, ax = make_line_chart(
            title = f"Median converge-cast delay when diameter={nice_value}",
            xdata = timeline,
            xlabel = unit_for(timeColumnName),
            ylabel = unit_for("Delay (s)"),
            ydata = {
                label_for(metric): (
                    converge_data.sel(diameter = diameter)[metric],
                    None #converge_error.sel(diameter = diameter)[metric].clip(0, diameter_value)
                )
                for metric in converge_data.data_vars #if not 'central' in metric
            },
            linewidth = 2
        )
        xmax = diameter_value*2.6
        ax.set_xlim(0, min(max(timeline), xmax))
        lim = diameter_value * 0.7
        ax.set_yscale('symlog', linthreshy=lim)
        ax.hlines(y=lim, xmin=0, xmax=xmax, color='black', linestyle='--', linewidth=1)
        ax.set_ylim(0, lim)
        ax.legend(ncol = 2)
        fig.tight_layout()
        fig.savefig(f'{output_directory}/converge_{nice_value}.pdf')
        plt.close(fig)

# Additional experiment
    leader_data = medians['leaderelection']
    last_time = max(leader_data['time'])
    last_time_data = leader_data.sel(time = last_time)

    def total_distance_for(algorithm, label = None):
        if label is None:
            label = algorithm
        last_time_data[r'$\overline{Distance}_{' + label + '}$ (hops)'] = last_time_data[f'HopCountClusterClosestNeighborDistance[leader-{algorithm}]'] + last_time_data[f'HopCountClusterIntraDistance[leader-{algorithm}]']

    total_distance_for('random')
    total_distance_for('pageRank', 'pagerank')
    total_distance_for('harmonic')
    total_distance_for('closeness')
    total_distance_for('degree')
    grains = leader_data['grain']
    for deployment in last_time_data['deploymentType']:
        deployment_data = last_time_data.sel(deploymentType = deployment)
        deployName = str(deployment.values)[:-1]
        for metric in last_time_data.data_vars:
            fig, ax = make_line_chart(
                title = f"{label_for(metric)} on a {deployName} topology",
                xdata = grains,
                xlabel = unit_for('grain'),
                ylabel = unit_for(metric),
                ydata = {
                    f'$n={beautifyValue(count)}$': (
                        deployment_data.sel(nodeCount = count)[metric],
                        None #converge_error.sel(diameter = diameter)[metric].clip(0, diameter_value)
                    )
                    for count in deployment_data['nodeCount']
                },
                linewidth = 2
            )
            ax.legend(ncol=3)
            fig.tight_layout()
            figname = f'leader_{deployName}_{metric}'
            for symbol in r".[]\/@:${}":
                figname = figname.replace(symbol, '_')
            fig.savefig(f'{output_directory}/leaderelection/{figname}.pdf')
