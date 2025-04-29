import logging

logger = logging.getLogger(__name__)

def executeNotebook(nb: str):
    """
    Executes the given Jupyter Notebook and returns the result
    """
    import nbformat
    from nbconvert.preprocessors import ExecutePreprocessor
    import json

    with open(nb) as ff:
        nb_in = nbformat.read(ff, nbformat.NO_CONVERT)

    ep = ExecutePreprocessor(timeout=600) #, kernel_name='python3')

    logger.info("Notebook execution", nb)
    nb_out = ep.preprocess(nb_in)
    logger.info("Notebook executed", nb)
    print(json.dumps(nb_out, indent=2))
    return nb_out